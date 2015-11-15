package gr.uoa.di.finer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import gr.uoa.di.finer.BuildConfig;
import gr.uoa.di.finer.ElectionActivity;
import gr.uoa.di.finer.MainActivity;
import gr.uoa.di.finer.R;
import gr.uoa.di.finer.Utils;

/**
 * A factory for notification objects.
 *
 * Instances of this class are thread-safe.
 *
 * @author Vasilis Poulimenos.
 */
class NotificationFactory {

    private static final String TAG = NotificationFactory.class.getName();

    private static final AtomicInteger requestCode = new AtomicInteger();

    // Caution: The identifiers for notifications must not be 0.
    private static final AtomicInteger notificationId = new AtomicInteger(1);


    /**
     * Returns a new unique notification ID for use with the {@link NotificationManager}.
     * @return a new unique notification ID
     */
    public static int getNotificationId() {
        return notificationId.getAndIncrement();
    }


    private final Context context;
    private final NotificationCompat.Builder normalBuilder;
    private final NotificationCompat.Builder ongoingBuilder;
    private final NotificationCompat.Builder errorBuilder;

    // TODO: Add actions to notifications (verification)?

    // Notifications and user navigation:
    // http://developer.android.com/training/notify-user/navigation.html

    /**
     * Constructs a new notification factory.
     *
     * @param context the context that the factory will use to create notifications
     */
    NotificationFactory(Context context) {
        this.context = context;
        // Notice that we don't set any defaults for ongoing notifications.
        this.normalBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_done_white_48dp)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);
        this.ongoingBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_play_arrow_white_48dp)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS);
        this.errorBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_error_white_48dp)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_ERROR);
    }


    private static final int PROGRESS_ZERO = 0;
    private static final int PROGRESS_MAX = 100;


    /**
     * Sets the content (title and text) for the notification.
     *
     * @param builder the builder that will be used to set the content
     * @param title the title of the notification
     * @param text the text of the notification
     */
    private void setContent(NotificationCompat.Builder builder,
                            CharSequence title, CharSequence text) {
        if (BuildConfig.DEBUG) Log.v(TAG, "Title: " + title + ", Text: " + text);
        builder.setContentTitle(title)
                .setContentText(text)
                .setTicker(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
    }

    /**
     *
     * @param builder the builder that will be used to set the content
     * @param titleId the resource ID for the title of the notification
     * @param textId the resource ID for the text of the notification
     * @param formatArgs the arguments that will be used to format the text
     */
    private void setContent(NotificationCompat.Builder builder,
                            @StringRes int titleId, @StringRes int textId, Object... formatArgs) {
        setContent(builder, context.getText(titleId), context.getString(textId, formatArgs));
    }


    /**
     *
     * @param count
     * @return
     */
    public Notification newOngoingInitNotification(String electionId, long count) {
        Intent notificationIntent = new Intent(context, ElectionActivity.class)
                .putExtra(ElectionActivity.EXTRA_ELECTION_ID, electionId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode.getAndIncrement(), notificationIntent, 0);
        synchronized(ongoingBuilder) {
            setContent(
                    ongoingBuilder,
                    context.getString(R.string.notification_ongoing_initialized_title, electionId),
                    context.getString(R.string.notification_ongoing_initialized_message, count));
            // Provide dummy values, since progress is set to indeterminate mode.
            return ongoingBuilder.setProgress(PROGRESS_ZERO, PROGRESS_ZERO, true)
                    .setContentIntent(pendingIntent)
                    .build();
        }
    }

    /**
     *
     * @return
     */
    public Notification newOngoingInitNotification(String electionId) {
        return newOngoingInitNotification(electionId, PROGRESS_ZERO);
    }

    /**
     *
     * @param count
     * @param max
     * @return
     */
    public Notification newOngoingVerNotification(String electionId, long count, long max) {
        Intent notificationIntent = new Intent(context, ElectionActivity.class)
                .putExtra(ElectionActivity.EXTRA_ELECTION_ID, electionId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode.getAndIncrement(), notificationIntent, 0);
        synchronized(ongoingBuilder) {
            setContent(
                    ongoingBuilder,
                    context.getString(R.string.notification_ongoing_verified_title, electionId),
                    context.getString(R.string.notification_ongoing_verified_message, count, max));
            // Convert to percentage, since setProgress accepts only int arguments.
            return ongoingBuilder.setProgress(PROGRESS_MAX, Utils.toPercentage(count, max), false)
                    .setContentIntent(pendingIntent)
                    .build();
        }
    }

    /**
     *
     * @return
     */
    public Notification newOngoingVerNotification(String electionId) {
        return newOngoingVerNotification(electionId, PROGRESS_ZERO, PROGRESS_MAX);
    }


    /**
     *
     * @param electionId
     * @param count
     * @return
     */
    public Notification newInitNotification(String electionId, long count) {
        //Intent notificationIntent = new Intent(context, ElectionActivity.class)
        Intent notificationIntent = new Intent(context, MainActivity.class)
                .putExtra(ElectionActivity.EXTRA_ELECTION_ID, electionId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode.getAndIncrement(), notificationIntent, 0);
        synchronized(normalBuilder) {
            setContent(
                    normalBuilder,
                    R.string.notification_initialized_title,
                    R.string.notification_initialized_message,
                    electionId,
                    count);
            return normalBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .build();
        }
    }

    /**
     * Creates a new notification for a successful election verification.
     *
     * @param electionId the ID of the successful election
     * @param ballotCount the number of voted ballots
     * @return
     */
    public Notification newVerNotification(String electionId, long ballotCount) {
        //Intent notificationIntent = new Intent(context, ElectionActivity.class)
        Intent notificationIntent = new Intent(context, MainActivity.class)
                .putExtra(ElectionActivity.EXTRA_ELECTION_ID, electionId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode.getAndIncrement(), notificationIntent, 0);
        synchronized(normalBuilder) {
            setContent(
                    normalBuilder,
                    R.string.notification_verified_title,
                    R.string.notification_verified_message,
                    electionId,
                    ballotCount);
            return normalBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .build();
        }
    }


    /**
     * Creates a new error notification for the specified election.
     *
     * @param titleId the resource ID for the title of the notification
     * @param textId the resource ID for the text of the notification
     * @param electionId
     * @return a new error notification
     */
    public Notification newErrorNotification(@StringRes int titleId, @StringRes int textId,
                                             String electionId) {
        //Intent errorIntent = new Intent(context, ElectionActivity.class)
        Intent errorIntent = new Intent(context, MainActivity.class)
                .putExtra(ElectionActivity.EXTRA_ELECTION_ID, electionId)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode.getAndIncrement(), errorIntent, 0);
        synchronized(errorBuilder) {
            setContent(
                    errorBuilder,
                    context.getString(titleId, electionId),
                    context.getText(textId));
            return errorBuilder.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentIntent(pendingIntent)
                    .build();
        }
    }

    /**
     * Creates a new generic error notification.
     *
     * @param titleId the resource ID for the title of the notification
     * @param textId the resource ID for the text of the notification
     * @return a new error notification
     */
    public Notification newErrorNotification(@StringRes int titleId, @StringRes int textId) {
        Intent errorIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode.getAndIncrement(), errorIntent, 0);
        synchronized(errorBuilder) {
            setContent(
                    errorBuilder,
                    context.getString(titleId),
                    context.getText(textId));
            return errorBuilder.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentIntent(pendingIntent)
                    .build();
        }
    }

}
