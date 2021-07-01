package com.makeability.protosound.ui.tutorial;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.hololo.tutorial.library.Step;
import com.hololo.tutorial.library.TutorialActivity;
import com.makeability.protosound.R;

public class Tutorial extends TutorialActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_1)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_2)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_3)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_4)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_5)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_6)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_7)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_8)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_9)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_10)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_11)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_12)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_13)
                .build());
        addFragment(new Step.Builder().setTitle("INSTRUCTIONS")
                .setBackgroundColor(Color.parseColor("#000000"))
                .setDrawable(R.drawable.tutorial_14)
                .build());
    }

    @Override
    public void currentFragmentPosition(int position) {

    }
}
