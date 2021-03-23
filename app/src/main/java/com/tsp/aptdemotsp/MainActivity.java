package com.tsp.aptdemotsp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.tsp.apt_annotation.BindView;
import com.tsp.apt_api.launcher.AutoBind;

public class MainActivity extends AppCompatActivity {


    @BindView(value = R.id.tv_auto)
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AutoBind.getInstance().inject(this);
        textView.setText("这是自定义绑定注解");
    }
}