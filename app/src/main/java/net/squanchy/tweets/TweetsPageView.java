package net.squanchy.tweets;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetui.SearchTimeline;
import com.twitter.sdk.android.tweetui.TimelineResult;

import net.squanchy.R;
import net.squanchy.tweets.view.TweetsAdapter;

import timber.log.Timber;

public class TweetsPageView extends LinearLayout {

    private TextView emptyView;
    private RecyclerView tweetsList;
    private TweetsAdapter tweetsAdapter;
    private SwipeRefreshLayout swipeLayout;
    private boolean refreshingData;

    public TweetsPageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TweetsPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TweetsPageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        super.setOrientation(VERTICAL);
    }

    @Override
    public void setOrientation(int orientation) {
        throw new UnsupportedOperationException(TweetsPageView.class.getSimpleName() + " doesn't support changing orientation");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        emptyView = (TextView) findViewById(R.id.empty_view);
        tweetsList = (RecyclerView) findViewById(R.id.tweet_feed);
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_container);

        swipeLayout.setOnRefreshListener(() -> {
            if (refreshingData) {
                Timber.i("Timeline refresh already underway, ignoring new refresh request");
                return;
            }
            refreshTimeline();
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initList();
    }

    private void initList() {

        Context context = getContext();
        String query = context.getString(R.string.social_query);

        if (!isInEditMode()) {
            SearchTimeline timeline = new SearchTimeline.Builder()
                    .query(query)
                    .build();

            tweetsAdapter = new TweetsAdapter(timeline, context);
            tweetsList.setAdapter(tweetsAdapter);
        }

        emptyView.setText(context.getString(R.string.no_tweets_for_query, query));
        refreshTimeline();
    }

    private void refreshTimeline() {
        swipeLayout.setRefreshing(true);
        refreshingData = true;
        tweetsAdapter.refresh(new TimelineLoadingCallback());
    }

    private void onRefreshFinished() {
        refreshingData = false;

        if (tweetsAdapter.isEmpty()) {
            emptyView.setVisibility(VISIBLE);
            tweetsList.setVisibility(GONE);
        } else {
            emptyView.setVisibility(GONE);
            tweetsList.setVisibility(VISIBLE);
        }

        swipeLayout.setRefreshing(false);
        tweetsAdapter.notifyDataSetChanged();
    }

    public class TimelineLoadingCallback extends Callback<TimelineResult<Tweet>> {

        @Override
        public void success(Result<TimelineResult<Tweet>> result) {
            onRefreshFinished();
        }

        @Override
        public void failure(TwitterException exception) {
            onRefreshFinished();
            Timber.e(exception, "Error while refreshing the timeline.");
        }
    }
}
