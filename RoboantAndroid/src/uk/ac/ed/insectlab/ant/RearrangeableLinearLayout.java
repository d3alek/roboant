package uk.ac.ed.insectlab.ant;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.LinearLayout;

public class RearrangeableLinearLayout extends LinearLayout {

	private static final String TAG = RearrangeableLinearLayout.class.getSimpleName();

	protected class myDragEventListener implements View.OnDragListener {

		// This is the method that the system calls when it dispatches a drag event to the
		// listener.
		public boolean onDrag(View v, DragEvent event) {

			final int action = event.getAction();

			switch(action) {

			case DragEvent.ACTION_DRAG_STARTED:
				//				Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
				//				v.startAnimation(shake);

				return(true);


			case DragEvent.ACTION_DRAG_ENTERED: 

				return(true);

			case DragEvent.ACTION_DRAG_LOCATION:

				return(true);


			case DragEvent.ACTION_DRAG_EXITED:


				return(true);


			case DragEvent.ACTION_DROP:

				return(true);

			case DragEvent.ACTION_DRAG_ENDED:

				// Turns off any color tinting
				v.clearAnimation();

				return(true);

			default:
				Log.e("DragDrop Example","Unknown action type received by OnDragListener.");

				break;
			};
			return false;
		}

	};

	public RearrangeableLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public void addView(View child) {
//		child.setOnDragListener(new myDragEventListener());
		super.addView(child);
	}


}
