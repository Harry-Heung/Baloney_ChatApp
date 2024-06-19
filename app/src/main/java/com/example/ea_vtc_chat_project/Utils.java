package com.example.ea_vtc_chat_project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

public class Utils {

    public static void applyRainbowTint(Context context, ImageView imageView) {
        int width = imageView.getWidth();
        int height = imageView.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        LinearGradient gradient = new LinearGradient(
                0, 0, width, 0,
                new int[]{0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF00FF00, 0xFF0000FF, 0xFF4B0082, 0xFF8B00FF},
                null,
                Shader.TileMode.CLAMP
        );
        paint.setShader(new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        paint.setShader(gradient);
        canvas.drawRect(0, 0, width, height, paint);

        imageView.setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
    }
}
