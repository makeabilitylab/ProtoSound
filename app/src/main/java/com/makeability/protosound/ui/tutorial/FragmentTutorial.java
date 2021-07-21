package com.makeability.protosound.ui.tutorial;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.makeability.protosound.R;

public class FragmentTutorial extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tutorial, container, false);
        final Button tutorialBtn = view.findViewById(R.id.tutorial_btn);
        tutorialBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("FragmentTutorial", "onClick called");
                startActivity(new Intent(FragmentTutorial.this.getActivity(), Tutorial.class));
            }
        });

        final Button faqBtn = view.findViewById(R.id.faq_btn);
        faqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("FragmentTutorial", "onClick called");
                startActivity(new Intent(FragmentTutorial.this.getActivity(), Faq.class));
                FragmentTutorial.this.getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        final Button aboutBtn = view.findViewById(R.id.about_btn);
        aboutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("FragmentTutorial", "onClick called");
                startActivity(new Intent(FragmentTutorial.this.getActivity(), About.class));
                FragmentTutorial.this.getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });
        return view;
    }
}
