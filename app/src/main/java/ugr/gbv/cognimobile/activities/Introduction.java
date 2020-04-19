package ugr.gbv.cognimobile.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import ugr.gbv.cognimobile.R;
import ugr.gbv.cognimobile.database.CognimobilePreferences;

public class Introduction extends Activity {
    private ViewPager2 viewPager;
    private int[] layouts;
    private Button next,skip;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduction);

        ViewPagerAdapter viewPagerAdapter;

        layouts = new int[]{
                R.layout.introduction_1,
                R.layout.introduction_2,
                R.layout.introduction_3
        };

        boolean go_out = true;
        if(getIntent().hasExtra("see-again")){
            go_out = false;
        }

        if(!CognimobilePreferences.getFirstTimeLaunch(getBaseContext()) &&
                go_out){
            goToMainMenu();
        }

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        viewPager = findViewById(R.id.view_pager);


        skip = findViewById(R.id.skip_button);
        if(!go_out)
            skip.setVisibility(View.INVISIBLE);
        next = findViewById(R.id.next_button);

        changeStatusBarColor();


        viewPagerAdapter = new ViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.registerOnPageChangeCallback(viewCallback);
        TabLayout tabLayout = findViewById(R.id.tabDots);
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText("")
        ).attach();
        next.setOnClickListener(view -> {
            int current = getItem();
            if(current< layouts.length){
                viewPager.setCurrentItem(current);
            }
            else {
                goToMainMenu();
            }

        });
        viewPager.setPageTransformer(new ZoomOutPageTransformer());
        skip.setOnClickListener(view -> goToMainMenu());
    }

    private int getItem(){
        return viewPager.getCurrentItem() + 1;
    }


    ViewPager2.OnPageChangeCallback viewCallback = new ViewPager2.OnPageChangeCallback() {

        @Override
        public void onPageSelected(int position) {
            if(CognimobilePreferences.getFirstTimeLaunch(getBaseContext())) {
                if (position == layouts.length - 1) {
                    next.setText(getResources().getString(R.string.proceed_button));
                    skip.setVisibility(View.GONE);
                } else {
                    next.setText(getResources().getString(R.string.next_button));
                    skip.setVisibility(View.VISIBLE);
                }
            }
            else{
                if (position == layouts.length - 1) {
                    next.setVisibility(View.INVISIBLE);
                } else {
                    if(next.getVisibility() == View.INVISIBLE)
                        next.setVisibility(View.VISIBLE);
                    next.setText(getResources().getString(R.string.next_button));
                }
            }
        }

    };


    private void changeStatusBarColor(){
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
    }


    public class ViewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        ViewPagerAdapter() {
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(viewType, parent, false);
            view.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new SliderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemViewType(int position) {
            return layouts[position];
        }

        @Override
        public int getItemCount() {
            return layouts.length;
        }

        class SliderViewHolder extends RecyclerView.ViewHolder {

            SliderViewHolder(View view) {
                super(view);
            }
        }
    }



    public class ZoomOutPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0f);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0f);
            }
        }
    }

    private void goToMainMenu(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
