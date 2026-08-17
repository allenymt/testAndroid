package com.vdian.android.lib.testforgradle.gradient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vdian.android.lib.testforgradle.R;
import com.vdian.android.lib.testforgradle.go_market.MarketLaunchHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yulun
 * @since 2023-11-14 18:42
 */
public class TestGradientActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_gradient);
        setTitle("TestGradientActivity");
        findViewById(R.id.test_gradient_view);
        setGradientView();
    }

    // 读取 R.drawable.test_gradient_img
    // "#667eea,#764ba2,#f093fb",按��号分割，解析成颜色数组，绘制成渐变背景覆盖在test_gradient_img上，渐变方向是从左上到右下
    // 注意test_gradient_img这张图片是带透明的，所以透明区域不能看到渐变，只能看到图片本身的颜色
    // 最终生成的图片设置成R.id.test_gradient_view的背景
    private void setGradientView() {
        try {
            // 读取原始图片
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.test_gradient_img);
            if (originalBitmap == null) {
                return;
            }
            
            // 解析颜色字符串
            String colorString = "#667eea,#764ba2,#f093fb";
            String[] colorStrings = colorString.split(",");
            List<Integer> colors = new ArrayList<>();
            for (String color : colorStrings) {
                colors.add(Color.parseColor(color.trim()));
            }
            
            // 创建与原图相同尺寸的画布
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(resultBitmap);
            
            // 创建渐变画笔（从左上到右下）
            Paint gradientPaint = new Paint();
            gradientPaint.setAntiAlias(true);
            int[] colorArray = new int[colors.size()];
            for (int i = 0; i < colors.size(); i++) {
                colorArray[i] = colors.get(i);
            }
            LinearGradient gradient = new LinearGradient(
                0, 0, width, height, // 从左上到右下
                colorArray,
                null, // 均匀分布
                Shader.TileMode.CLAMP
            );
            gradientPaint.setShader(gradient);
            
            // 绘制渐变背景
            canvas.drawRect(0, 0, width, height, gradientPaint);
            
            // 创建混合模式画笔，只在原图非透明区域显示渐变
            Paint maskPaint = new Paint();
            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            
            // 将原图作为遮罩绘制，实现只在非透明区域显示渐变
            canvas.drawBitmap(originalBitmap, 0, 0, maskPaint);
            
            // 设置为View的背景
            View gradientView = findViewById(R.id.test_gradient_view);
            if (gradientView != null) {
                BitmapDrawable drawable = new BitmapDrawable(getResources(), resultBitmap);
                gradientView.setBackground(drawable);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
