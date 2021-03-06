package ugr.gbv.cognimobile.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONException;

import java.util.ArrayList;

import ugr.gbv.cognimobile.R;
import ugr.gbv.cognimobile.interfaces.LoadContent;
import ugr.gbv.cognimobile.interfaces.LoadDraw;
import ugr.gbv.cognimobile.utilities.ContextDataRetriever;
import ugr.gbv.cognimobile.utilities.DrawingView;
import ugr.gbv.cognimobile.utilities.PathGenerator;
import ugr.gbv.cognimobile.utilities.Point;

/**
 * Class to display the task type "Draw":
 * {@link Task#GRAPH}
 * {@link Task#CUBE}
 * {@link Task#WATCH}
 */
public class DrawTask extends Task implements LoadDraw {


    private DrawingView drawingView;
    private ArrayList<Point> sequence;
    private ArrayList<String> answer;
    private View view;
    private LinearLayout leftButtonContainer;
    private int undoTimes;
    private Bundle bundle;

    private final ArrayList<Button> pressedButtons;
    private final ArrayList<String> alreadyPressedButtons;
    private final ArrayList<Long> timeBetweenClicks;


    /**
     * Constructor
     *
     * @param taskType type of the task
     * @param callBack callback to pass the parent the events
     * @param bundle   bundle of information to be filled into the task
     */
    public DrawTask(int taskType, LoadContent callBack, @Nullable Bundle bundle) {
        this.taskType = taskType;
        pressedButtons = new ArrayList<>();
        alreadyPressedButtons = new ArrayList<>();
        timeBetweenClicks = new ArrayList<>();
        this.callBack = callBack;
        if (bundle != null) {
            this.bundle = bundle;
        }
    }


    /**
     * Overrides {@link androidx.fragment.app.Fragment#onViewCreated(View, Bundle)}
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Handler handler = new Handler();
        handler.post(this::shouldDisplayHelpAtBeginning);
    }

    /**
     * Overrides {@link androidx.fragment.app.Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * Also sets all the necessary elements for the task to be displayed and be completed.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.draw_task, container, false);
        leftButtonContainer = view.findViewById(R.id.leftButtonContainer);
        leftButtonContainer.setVisibility(View.INVISIBLE);
        drawingView = view.findViewById(R.id.drawingSpace);
        banner = view.findViewById(R.id.banner);
        mainLayout = view.findViewById(R.id.drawTaskLayout);


        switch (taskType){
            case GRAPH:
                drawingView.setCallBack(this, false);
                break;
            case CUBE:
            case WATCH:
                drawingView.setCallBack(this, true);
                break;
            default:
                throw new RuntimeException("Task type unsupported");
        }

        context = getContext();

        undoTimes = getResources().getInteger(R.integer.undo_times);

        centerButton = view.findViewById(R.id.centerButton);

        buildDialog();


        rightButton = view.findViewById(R.id.rightButton);


        leftButton = view.findViewById(R.id.leftButton);
        leftButton.setOnClickListener(view -> {
            if(checkIfHasUndoActions()) {
                decrementUndoAction();
                drawingView.undoLastOperation();
                if (taskType == GRAPH) {
                    undoLastButton();
                }
            }
            showUndoTimes();
        });

        answer = new ArrayList<>();
        providedTask = true;

        setNextButtonStandardBehaviour();

        displayHelpAtBeginning = bundle.getBoolean("display_help");


        return view;
    }

    /**
     * Decrements the undoTimes variable
     */
    private void decrementUndoAction() {
        undoTimes--;
    }

    /**
     * Checks if the user can click the undo button
     *
     * @return true if the user has the possibility, false if not.
     */
    private boolean checkIfHasUndoActions() {
        return undoTimes > 0;
    }

    /**
     * Restores the last button, only on task type {@link Task#GRAPH}.
     */
    private void undoLastButton() {

        if (answer.size() > 0) {
            answer.remove(answer.size() - 1);
            pressedButtons.get(pressedButtons.size() - 1).setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.circle_no_fill, context.getTheme()));
            pressedButtons.get(pressedButtons.size() - 1).setTextColor(getResources().getColor(R.color.black, context.getTheme()));
            pressedButtons.remove(pressedButtons.size() - 1);
        }

    }

    /**
     * Displays how many attempts the user has left to push the undo button.
     */
    private void showUndoTimes() {
        if (undoTimes > 0)
            Toast.makeText(context, getResources().getString(R.string.times_left, undoTimes), Toast.LENGTH_LONG).show();
        else
            Toast.makeText(context, getResources().getString(R.string.no_times_left), Toast.LENGTH_LONG).show();
    }

    /**
     * Draws on top of the canvas the buttons to be clicked, only on task type {@link Task#GRAPH}.
     *
     * @param points Coordinates where the points will be drawn.
     */
    private void drawButtons(ArrayList<Point> points) {
        CardView layout = view.findViewById(R.id.cardView);

        String[] tags = context.getResources().getStringArray(R.array.graphsValues);


        for (int i = 0; i < points.size(); ++i) {
            Button button = new Button(context);
            button.setId(View.generateViewId());
            button.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.circle_no_fill, context.getTheme()));
            button.setText(tags[i]);
            button.setTag(points.get(i).getLabel());
            button.setOnClickListener(v -> {
                Button button1 = (Button) v;
                boolean continua = true;
                for (int i1 = 0; i1 < sequence.size() && continua; ++i1) {
                    if (button1.getTag().equals(sequence.get(i1).getLabel())) {
                        if(!answer.contains(button1.getTag().toString())){
                            button1.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.circle_with_fill, context.getTheme()));
                            button1.setTextColor(getResources().getColor(R.color.white,context.getTheme()));
                            answer.add(button1.getTag().toString());
                            drawingView.drawToPoint(sequence.get(i1));
                            continua = false;
                            pressedButtons.add(button1);
                            timeBetweenClicks.add(ContextDataRetriever.addTimeStamp());
                            startedTask();
                        } else {
                            alreadyPressedButtons.add(button1.getTag().toString());
                        }
                    }
                }

            });
            layout.addView(button);
            int dimens = getResources().getDimensionPixelSize(R.dimen.circle);
            int halfDimen = dimens / 2;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dimens, dimens);
            button.setLayoutParams(lp);
            button.setX(points.get(i).getX() - halfDimen);
            button.setY(points.get(i).getY() - halfDimen);


        }


    }


    /**
     * Overrides {@link LoadDraw#loadDraw()}
     */
    @Override
    public void loadDraw() {
        if (!loaded) {

            ImageView imageView = view.findViewById(R.id.banner_image);
            bannerText = view.findViewById(R.id.banner_text);

            TextView label = view.findViewById(R.id.leftButtonLabel);
            switch (taskType) {
                case GRAPH:
                    int height = drawingView.getCanvasHeight();
                    int width = drawingView.getCanvasWidth();
                    PathGenerator pathGenerator = new PathGenerator();
                    sequence = pathGenerator.makePath(height, width);
                    drawButtons(sequence);
                    try {
                        callBack.getJsonAnswerWrapper().addIntArray("pattern_sequence", pathGenerator.getNumbers());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    bannerText.setText(R.string.graph_instructions);
                    break;
                case CUBE:
                    leftButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_delete_forever_black_24dp, context.getTheme()));
                    label.setText(R.string.clear_title_button);
                    bannerText.setText(R.string.cube_instructions);
                    imageView.setVisibility(View.VISIBLE);
                    break;
                case WATCH:
                    leftButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.ic_delete_forever_black_24dp, context.getTheme()));
                    label.setText(R.string.clear_title_button);
                    bannerText.setText(context.getResources().getString(R.string.clock_instructions, bundle.getString("hour")));
                    break;
                default:
                    throw new RuntimeException("Task type not supported");
            }

            leftButtonContainer.setVisibility(View.VISIBLE);
            loaded = true;
        }
    }

    /**
     * Overrides {@link LoadDraw#startedTask()}
     * This determines if a task has been started to skip it without having a confirmation
     * {@link Task#setNextButtonStandardBehaviour()} has the logic of that confirmation
     */
    @Override
    public void startedTask() {
        taskEnded = true;
    }


    /**
     * Overrides {@link Task#saveResults()}
     */
    @Override
    void saveResults() throws JSONException {

        callBack.getJsonAnswerWrapper().addField("task_type", taskType);
        callBack.getJsonAnswerWrapper().addField("height", drawingView.getCanvasHeight());
        callBack.getJsonAnswerWrapper().addField("width", drawingView.getCanvasWidth());
        callBack.getJsonAnswerWrapper().addFloatArray("points_sequence", drawingView.getDrawnPath());


        switch (taskType) {
            case GRAPH:
                setScoring();
                callBack.getJsonAnswerWrapper().addArrayList("answer_sequence", answer);
                callBack.getJsonAnswerWrapper().addField("score",score);
                callBack.getJsonAnswerWrapper().addFloatArray("erased_paths", drawingView.getErasedPath());


                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificATMAlreadyClickedButton, ContextDataRetriever.retrieveInformationFromStringArrayList(alreadyPressedButtons));
                callBack.getJsonContextEvents().addFloatArray(ContextDataRetriever.SpecificATMPoints, drawingView.getDrawnPath());
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificATMDistanceBetweenCircles, ContextDataRetriever.retrieveInformationFromButtonArrayList(pressedButtons));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificATMTimeBetweenClicks, ContextDataRetriever.retrieveInformationFromLongArrayList(timeBetweenClicks));
                break;
            case CUBE:
                callBack.getJsonAnswerWrapper().addField("times_wipe_canvas", getResources().getInteger(R.integer.undo_times) - undoTimes);
                Pair<String, String> cubeTraces = packTraces();
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificVSCubeStartDraw, cubeTraces.first);
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificVSCubeEndDraw, cubeTraces.second);
                callBack.getJsonContextEvents().addFloatArray(ContextDataRetriever.SpecificVSCubePoints, drawingView.getDrawnPath());
                break;
            case WATCH:
                callBack.getJsonAnswerWrapper().addField("times_wipe_canvas", getResources().getInteger(R.integer.undo_times) - undoTimes);
                Pair<String, String> clockTraces = packTraces();
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificVSClockStartDraw, clockTraces.first);
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificVSClockEndDraw, clockTraces.second);
                callBack.getJsonContextEvents().addFloatArray(ContextDataRetriever.SpecificVSClockPoints, drawingView.getDrawnPath());
                break;
            default:
                throw new RuntimeException("INVALID TASKTYPE");

        }


    }


    /**
     * Converts the points where the traces(starting point and end point) that user has drawn
     * on the canvas.
     *
     * @return a pair, where the first one is the first positions the user has drawn (x:y) and
     * on the second element of the pair is the last positions the user has drawn (x:y)
     */
    private Pair<String, String> packTraces() {
        float[] drawnTraces = drawingView.getDrawnTraces();
        StringBuilder startStringBuilder = new StringBuilder();
        StringBuilder endStringBuilder = new StringBuilder();
        for (int i = 0, k = 0; k < drawnTraces.length; ++i, k += 2) {
            if (i % 2 == 0) {
                startStringBuilder.append(drawnTraces[k]);
                startStringBuilder.append(":");
                startStringBuilder.append(drawnTraces[k + 1]);
                startStringBuilder.append(",");
            } else {
                endStringBuilder.append(drawnTraces[k]);
                endStringBuilder.append(":");
                endStringBuilder.append(drawnTraces[k + 1]);
                endStringBuilder.append(",");
            }

        }
        if (startStringBuilder.length() > 0)
            startStringBuilder.deleteCharAt(startStringBuilder.length() - 1);
        if (endStringBuilder.length() > 0)
            endStringBuilder.deleteCharAt(endStringBuilder.length() - 1);
        return new Pair<>(startStringBuilder.toString(), endStringBuilder.toString());
    }

    /**
     * Overrides {@link Task#setScoring()}
     */
    @Override
    void setScoring() {
        int size = answer.size();
        boolean goOn = true;
        score = size > 0 ? 1 : 0;
        for (int i = 0; i < size && goOn; ++i)
            if (!answer.get(i).equals(Integer.toString(i + 1))) {
                goOn = false;
                score = 0;
            }
    }
}
