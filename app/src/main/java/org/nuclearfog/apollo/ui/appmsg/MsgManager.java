package org.nuclearfog.apollo.ui.appmsg;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Evgeny Shishkin
 */
class MsgManager extends Handler {

	private static final int MESSAGE_DISPLAY = 0xc2007;
	private static final int MESSAGE_ADD_VIEW = 0xc20074dd;
	private static final int MESSAGE_REMOVE = 0xc2007de1;

	private static final MsgManager INSTANCE = new MsgManager();

	private Queue<WeakReference<AppMsg>> msgQueue = new LinkedList<>();
	private Animation inAnimation, outAnimation;

	/**
	 *
	 */
	private MsgManager() {
		super(Looper.getMainLooper());
	}

	/**
	 * @return The currently used instance of the {@link MsgManager}.
	 */
	static synchronized MsgManager getInstance() {
		return INSTANCE;
	}


	@Override
	public void handleMessage(Message msg) {
		AppMsg appMsg;
		switch (msg.what) {
			case MESSAGE_DISPLAY:
				displayMsg();
				break;

			case MESSAGE_ADD_VIEW:
				appMsg = (AppMsg) msg.obj;
				addMsgToView(appMsg);
				break;

			case MESSAGE_REMOVE:
				appMsg = (AppMsg) msg.obj;
				removeMsg(appMsg);
				break;

			default:
				super.handleMessage(msg);
				break;
		}
	}

	/**
	 * Inserts a {@link AppMsg} to be displayed.
	 */
	void add(AppMsg appMsg) {
		msgQueue.add(new WeakReference<>(appMsg));
		if (inAnimation == null) {
			inAnimation = AnimationUtils.loadAnimation(appMsg.getContext(), android.R.anim.fade_in);
		}
		if (outAnimation == null) {
			outAnimation = AnimationUtils.loadAnimation(appMsg.getContext(), android.R.anim.fade_out);
		}
		displayMsg();
	}

	/**
	 * Displays the next {@link AppMsg} within the queue.
	 */
	private void displayMsg() {
		WeakReference<AppMsg> ref = msgQueue.peek();
		if (ref != null) {
			// First peek whether the AppMsg is being displayed.
			AppMsg appMsg = ref.get();
			if (appMsg != null) {
				Message msg;
				if (!appMsg.isShowing()) {
					// Display the AppMsg
					msg = obtainMessage(MESSAGE_ADD_VIEW);
					msg.obj = appMsg;
					sendMessage(msg);
				} else {
					msg = obtainMessage(MESSAGE_DISPLAY);
					sendMessageDelayed(msg, appMsg.getDuration() + inAnimation.getDuration() + outAnimation.getDuration());
				}
			} else {
				// remove item if reference is null
				msgQueue.poll();
			}
		}
	}

	/**
	 * Removes the {@link AppMsg}'s view after it's display duration.
	 *
	 * @param appMsg The {@link AppMsg} added to a {@link ViewGroup} and should be removed.s
	 */
	private void removeMsg(AppMsg appMsg) {
		ViewGroup parent = ((ViewGroup) appMsg.getView().getParent());
		if (parent != null) {
			appMsg.getView().startAnimation(outAnimation);
			// Remove the AppMsg from the queue.
			msgQueue.poll();
			// Remove the AppMsg from the view's parent.
			parent.removeView(appMsg.getView());
			Message msg = obtainMessage(MESSAGE_DISPLAY);
			sendMessage(msg);
		}
	}

	/**
	 *
	 */
	private void addMsgToView(AppMsg appMsg) {
		if (appMsg.getView().getParent() == null) {
			appMsg.addContentView(appMsg.getView());
		}
		appMsg.getView().startAnimation(inAnimation);
		Message msg = obtainMessage(MESSAGE_REMOVE);
		msg.obj = appMsg;
		sendMessageDelayed(msg, appMsg.getDuration());
	}
}