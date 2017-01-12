package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.mokee.utils.MoKeeUtils.
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.R;

public class SettingItemCheck extends LinearLayout {

    private ImageView mCheckImage;
    private TextView mTitle;
    private TextView mSummary;
    private ImageView mIcon;
    private View mTextLayout;
    private Drawable mIconDrawable;

    public SettingItemCheck(Context context) {
        this(context, null);
    }

    public SettingItemCheck(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingItemCheck(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final View container = LayoutInflater.from(context).inflate(R.layout.setting_item_check_layout, this, true);

        mTextLayout = findViewById(R.id.text_layout);
        mTitle = (TextView) container.findViewById(R.id.item_switch_title);
        mSummary = (TextView) container.findViewById(R.id.item_summary);
        mCheckImage = (ImageView) container.findViewById(R.id.item_check);
        mIcon = (ImageView) container.findViewById(R.id.icon);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingItemCheck, defStyle, 0);

        int titleSize = a.getDimensionPixelSize(R.styleable.SettingItemCheck_itemTitleSize, -1);
        if (titleSize > 0) {
            mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize);
        }

        mTitle.setText(a.getText(R.styleable.SettingItemCheck_itemTitle));

        a.recycle();
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mIconDrawable != null) {
            int iconWidth = mIconDrawable.getIntrinsicWidth();
            int iconLeftMargin = getResources().getDimensionPixelOffset(R.dimen.settings_item_icon_left_margin);
            mTextLayout.setTranslationX(iconWidth + iconLeftMargin);
        }
        if(Utils.isTextEllipsized(mTitle)){
            setMaxTitleSize(getContext().getResources().getDimensionPixelSize(R.dimen.common_max_size));
        }
    }

    public ImageView getIcon() {
        return mIcon;
    }

    public void setIcon(int res) {
        mIcon.setImageResource(res);
        setIconVisibility(View.VISIBLE);
    }

    public void setIcon(Drawable drawable) {
        mIcon.setImageDrawable(drawable);
        setIconVisibility(View.VISIBLE);
    }

    public void setIconVisibility(int visiableId) {
        if (visiableId == View.VISIBLE || visiableId == View.INVISIBLE || visiableId == View.GONE) {
            mIcon.setVisibility(visiableId);
        } else {
            throw new IllegalArgumentException("the params for setIconVisibility method is illegal!");
        }
    }

    /**
     * @param maxTextSize unit px
     */
    public void setMaxTitleSize(float maxTextSize) {
        MoKeeUtils.setMaxTextSizeForTextView(mTitle, maxTextSize);
    }

    /**
     * @param maxTextSize unit px
     */
    public void setMaxSummarySize(float maxTextSize) {
        MoKeeUtils.setMaxTextSizeForTextView(mSummary, maxTextSize);
    }

    public TextView getTitleView() {
        return mTitle;
    }

    public void setTitle(CharSequence str) {
        MoKeeUtils.resetTextViewFontSizeAttr(getContext(), mTitle, R.dimen.settings_item_title_size);
        mTitle.setText(str);
    }

    public void setTitle(int resId) {
        mTitle.setText(getResources().getString(resId));
    }

    public TextView getSummaryView() {
        return mSummary;
    }

    public void setSummary(CharSequence str) {
        mSummary.setText(str);
        mSummary.setVisibility(TextUtils.isEmpty(str) ? View.GONE : View.VISIBLE);
    }

    public void setSummaryMarqueeEnable(boolean enable) {
        mSummary.setEllipsize(enable ? TextUtils.TruncateAt.MARQUEE : TextUtils.TruncateAt.END);
        mSummary.setFocusable(enable);
        mSummary.setFocusableInTouchMode(enable);
    }

    public void setChecked(boolean isChecked) {
        mCheckImage.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
    }

    public boolean isChecked() {
        return mCheckImage.getVisibility() == View.VISIBLE;
    }

    public void setCheckedIconLight(boolean light) {
        if (light) {
            mCheckImage.setImageResource(R.drawable.selector_check_icon_light);
        } else {
            mCheckImage.setImageResource(R.drawable.selector_check_icon);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCheckedIconLight(!enabled);
        mCheckImage.setEnabled(enabled);
        mTitle.setAlpha(enabled ? 1.0f : 0.3f);
        mIcon.setEnabled(enabled);
        mIcon.setAlpha(enabled ? 1.0f : 0.3f);
    }
}
