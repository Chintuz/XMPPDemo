package com.xmpp.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.xmpp.Utils.CommonMethods;
import com.xmpp.adapter.ChatAdapter;
import com.xmpp.demo.MainActivity;
import com.xmpp.demo.R;
import com.xmpp.model.ChatMessage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by admin on 05/03/2016.
 */
public class Chats extends Fragment implements View.OnClickListener {

    private EditText msg_edittext;
    private String user1 = "chikuz", user2 = "sandy009";
    private Random random;
    public static ArrayList<ChatMessage> chatlist;
    public static ChatAdapter chatAdapter;
    ListView msgListView;

    private final int REQUEST_PICK_GALLERY = 100;
    private final int REQUEST_TAKE_CAMERA = 200;
    private Uri mImageCaptureUri;
    private String PREF_IMAGE_URI = "image_uri";
    private Bitmap bitmapPic;
    private File file;
    private boolean isCamera = false;
    private boolean isGallery = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chat_layout, container, false);
        random = new Random();
//        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle("Chats");
        msg_edittext = (EditText) view.findViewById(R.id.messageEditText);
        msgListView = (ListView) view.findViewById(R.id.msgListView);
        ImageButton sendButton = (ImageButton) view.findViewById(R.id.sendMessageButton);
        sendButton.setOnClickListener(this);

        // ----Set autoscroll of listview when a new message arrives----//
        msgListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        msgListView.setStackFromBottom(true);

        chatlist = new ArrayList<ChatMessage>();
        chatAdapter = new ChatAdapter(getActivity(), chatlist);
        msgListView.setAdapter(chatAdapter);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }

    public void sendTextMessage(View v) {
        String message = msg_edittext.getEditableText().toString();
        if (!message.equalsIgnoreCase("")) {
            final ChatMessage chatMessage = new ChatMessage(user1, user2, message, "", "" + random.nextInt(1000), true);
            chatMessage.setMsgID();
            chatMessage.body = message;
            chatMessage.Date = CommonMethods.getCurrentDate();
            chatMessage.Time = CommonMethods.getCurrentTime();
            msg_edittext.setText("");
            chatAdapter.add(chatMessage);
            chatAdapter.notifyDataSetChanged();
            MainActivity activity = ((MainActivity) getActivity());
            activity.getmService().xmpp.sendMessage(chatMessage);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendMessageButton:
                if (!msg_edittext.getText().toString().trim().isEmpty()) {
                    sendTextMessage(v);
                } else {
                    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, REQUEST_PICK_GALLERY);
                }
        }
    }


    /*private void imageChooser() {
        final String[] items = new String[]{"Camera", "Gallery"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.select_dialog_item, items);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select From");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) { // pick from
                if (item != 0) {
                    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, REQUEST_PICK_GALLERY);
                } else if (Build.VERSION.SDK_INT < 19) {
                    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    captureIntent.putExtra("return-data", true);
                    startActivityForResult(captureIntent, REQUEST_TAKE_CAMERA);
                } else {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "tmp_avatar_"
                            + String.valueOf(System.currentTimeMillis()) + ".jpg"));
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
                    Utils.saveStringInPref(getActivity(), PREF_IMAGE_URI, mImageCaptureUri.toString());
                    try {
                        intent.putExtra("return-data", true);
                        startActivityForResult(intent, REQUEST_TAKE_CAMERA);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        Dialog dialog = builder.create();
        dialog.show();
    }*/


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != getActivity().RESULT_OK)
            return;
        switch (requestCode) {
            case REQUEST_PICK_GALLERY:
                isGallery = true;
                isCamera = false;
                mImageCaptureUri = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                Cursor cursor = getActivity().getApplicationContext().getContentResolver().query(mImageCaptureUri, filePath, null, null, null);
                cursor.moveToFirst();
                cursor.close();
                try {
                    final ChatMessage chatMessage = new ChatMessage(user1, user2, "", "", getRealPathFromURI(mImageCaptureUri) + random.nextInt(1000), true);
                    chatMessage.setMsgID();
                    chatMessage.body = "";
                    chatMessage.Date = CommonMethods.getCurrentDate();
                    chatMessage.Time = CommonMethods.getCurrentTime();
                    chatMessage.imagePath = getRealPathFromURI(mImageCaptureUri);
                    msg_edittext.setText("");
                    chatAdapter.add(chatMessage);
                    chatAdapter.notifyDataSetChanged();
//                    file = new File(getRealPathFromURI(mImageCaptureUri));
                    MainActivity activity = ((MainActivity) getActivity());
                    activity.getmService().xmpp.sendImageFile(getRealPathFromURI(mImageCaptureUri));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            default:
                try {
                    if (new File(mImageCaptureUri.getPath()).exists())
                        new File(mImageCaptureUri.getPath()).delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public static void saveStringInPref(Context activity, String key, String value) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(key, value);
        edit.commit();
    }


    public static String getStringPref(Context activity, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String restoredText = prefs.getString(key, "");
        return restoredText;
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Audio.Media.DATA};
        Cursor cursor = getActivity().managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }


    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private Bitmap rotateImage() {
        // TODO Auto-generated method stub
        try {
            ExifInterface ei = new ExifInterface(mImageCaptureUri.getPath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    bitmapPic = RotateBitmap(bitmapPic, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    bitmapPic = RotateBitmap(bitmapPic, 180);
                    break;
                // etc.
            }
        } catch (IOException e) {
        }
        return bitmapPic;
    }


}