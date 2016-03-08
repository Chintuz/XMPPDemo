package com.xmpp.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xmpp.demo.R;
import com.xmpp.model.ChatMessage;

import java.util.ArrayList;

/**
 * Created by admin on 05/03/2016.
 */
public class ChatAdapter extends BaseAdapter {

    private static LayoutInflater inflater = null;
    ArrayList<ChatMessage> chatMessageList;
    BitmapFactory.Options bmOptions;

    public ChatAdapter(Activity activity, ArrayList<ChatMessage> list) {
        chatMessageList = list;
        bmOptions = new BitmapFactory.Options();
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return chatMessageList.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage message = (ChatMessage) chatMessageList.get(position);
        View vi = convertView;
        ViewHolder holder = null;

        if (convertView == null) {
            vi = inflater.inflate(R.layout.chatbubble, null);
            holder = new ViewHolder();
            holder.msg = (TextView) vi.findViewById(R.id.message_text);
            holder.img = (ImageView) vi.findViewById(R.id.message_img);
            holder.layout = (RelativeLayout) vi.findViewById(R.id.bubble_layout);
            holder.parent_layout = (LinearLayout) vi.findViewById(R.id.bubble_layout_parent);
            vi.setTag(holder);
        } else {
            holder = (ViewHolder) vi.getTag();
        }

        // if message is mine then align to right
        if (message.isMine) {
            holder.layout.setBackgroundResource(R.drawable.bubble2);
            holder.parent_layout.setGravity(Gravity.RIGHT);
        }
        // If not mine then align to left
        else {
            holder.layout.setBackgroundResource(R.drawable.bubble1);
            holder.parent_layout.setGravity(Gravity.LEFT);
        }


        if (message.imagePath.equalsIgnoreCase("")) {
            holder.msg.setVisibility(View.VISIBLE);
            holder.img.setVisibility(View.GONE);
            holder.msg.setText(message.body);
            holder.msg.setTextColor(Color.BLACK);

        } else if (!message.imagePath.equalsIgnoreCase("")) {
            holder.msg.setVisibility(View.GONE);
            holder.img.setVisibility(View.VISIBLE);
            holder.img.setImageBitmap(ShrinkBitmap(message.imagePath, 200, 200));
        }

        return vi;
    }

    public void add(ChatMessage object) {
        chatMessageList.add(object);
    }

    public class ViewHolder {
        ImageView img;
        TextView msg;
        RelativeLayout layout;
        LinearLayout parent_layout;
    }

    Bitmap ShrinkBitmap(String file, int width, int height){

        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);

        int heightRatio = (int)Math.ceil(bmpFactoryOptions.outHeight/(float)height);
        int widthRatio = (int)Math.ceil(bmpFactoryOptions.outWidth/(float)width);

        if (heightRatio > 1 || widthRatio > 1)
        {
            if (heightRatio > widthRatio)
            {
                bmpFactoryOptions.inSampleSize = heightRatio;
            } else {
                bmpFactoryOptions.inSampleSize = widthRatio;
            }
        }

        bmpFactoryOptions.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
        return bitmap;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }
}