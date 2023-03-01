package com.vdian.android.lib.testforgradle.activityResult.lauchmodel;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vdian.android.lib.testforgradle.R;

/**
 * @author yulun
 * @sinice 2020-12-07 15:37
 * https://www.jianshu.com/p/f7b61e4f7782
 * 例如买家版中MainTabsActivity的启动模式是singleTask,如果MainTabsActivity启动页面A，
 * A再通过startStartActivityForResult启动MainTabsActivity，
 * 此时singleTask是失效的，MainTabsActivity会重新建一个实例
 *
 */
public class TestSingleTaskActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(R.anim.flutter_hybrid_top_in, R.anim.flutter_hybrid_exit_bottom);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_single_task);
        setTitle("TestSingleTaskActivity");
        findViewById(R.id.test_standard_launch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(TestSingleTaskActivity.this, TestStandardActivity.class));
            }
        });

        findViewById(R.id.test_result_launch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(TestSingleTaskActivity.this, TestStandardActivity.class), 1000);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }


    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.flutter_hybrid_enter_bottom, R.anim.flutter_hybrid_top_out);
    }
}
