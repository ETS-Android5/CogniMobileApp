package ugr.gbv.cognimobile.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ugr.gbv.cognimobile.R;
import ugr.gbv.cognimobile.adapters.WordListAdapter;
import ugr.gbv.cognimobile.database.CognimobilePreferences;
import ugr.gbv.cognimobile.interfaces.LoadContent;
import ugr.gbv.cognimobile.interfaces.TTSHandler;
import ugr.gbv.cognimobile.interfaces.TextTaskCallback;
import ugr.gbv.cognimobile.utilities.ContextDataRetriever;
import ugr.gbv.cognimobile.utilities.ErrorHandler;
import ugr.gbv.cognimobile.utilities.TextToSpeechLocal;

import static android.app.Activity.RESULT_OK;
/**
 * Class to display the task type "Text":
 * {@link Task#MEMORY}
 * {@link Task#ATTENTION_NUMBERS}
 * {@link Task#ATTENTION_LETTERS}
 * {@link Task#ATTENTION_SUBTRACTION}
 * {@link Task#LANGUAGE}
 * {@link Task#FLUENCY}
 * {@link Task#ABSTRACTION}
 * {@link Task#RECALL}
 * {@link Task#ORIENTATION}
 */
public class TextTask extends Task implements TTSHandler, TextTaskCallback {

    //CHECKERS VARS
    private String[] array;
    private ArrayList<String> answers;
    private ArrayList<String> expectedAnswers;

    //UI
    private View mainView;
    private RelativeLayout playableArea;
    private TextView countdownText;
    private TextView additionalTaskText;
    private LinearLayout submitAnswerContainer;
    private RecyclerView recyclerView;
    private WordListAdapter adapter;
    private LinearLayout sttButtonContainer;
    private ExtendedFloatingActionButton startButton;
    private ArrayList<EditText> variousInputs;
    private Bundle bundle;
    private AlertDialog progressDialog;

    //MEMORY Y RECALL
    private ArrayList<Long> scrollingTimes;
    private ArrayList<Long> settlingTimes;
    //NUMBERS
    private ArrayList<Integer> positionFilling;
    //LETTERS
    private ArrayList<Long> soundTimes;
    private ArrayList<Long> clickingTimes;


    //FLAGS
    private int timesCompleted;
    private boolean onlyNumbersInputAccepted;
    private boolean firstDone;

    /**
     * Constructor
     *
     * @param taskType type of the task
     * @param callBack callback to pass the parent the events
     * @param bundle   bundle of information to be filled into the task
     */
    public TextTask(int taskType, LoadContent callBack, @NonNull Bundle bundle) {
        this.callBack = callBack;
        this.taskType = taskType;
        answers = new ArrayList<>();
        expectedAnswers = new ArrayList<>();
        scrollingTimes = new ArrayList<>();
        settlingTimes = new ArrayList<>();
        positionFilling = new ArrayList<>();
        soundTimes = new ArrayList<>();
        clickingTimes = new ArrayList<>();
        timesCompleted = 0;
        firstDone = false;
        this.bundle = bundle;
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

        mainView = inflater.inflate(R.layout.text_task, container, false);

        context = getContext();
        parent = getActivity();

        onlyNumbersInputAccepted = false;

        // Assignation of variables
        playableArea = mainView.findViewById(R.id.textSpace);
        mainLayout = mainView.findViewById(R.id.textTaskLayout);
        additionalTaskText = mainView.findViewById(R.id.additional_task_text);
        firstInput = mainView.findViewById(R.id.additional_task_input);
        submitAnswerButton = mainView.findViewById(R.id.submitButton);
        bannerText = mainView.findViewById(R.id.banner_text);
        FloatingActionButton sttButton = mainView.findViewById(R.id.sttButton);
        startButton = mainLayout.findViewById(R.id.startButton);
        sttButtonContainer = mainLayout.findViewById(R.id.sttButtonContainer);
        submitAnswerContainer = mainLayout.findViewById(R.id.submitButtonContainer);
        rightButton = mainView.findViewById(R.id.rightButton);
        banner = mainView.findViewById(R.id.banner);
        countdownText = mainView.findViewById(R.id.countDownText);
        centerButton = mainView.findViewById(R.id.centerButton);

        firstInput.setFocusableInTouchMode(true);

        // Assignation of recyclerview
        recyclerView = mainView.findViewById(R.id.words_list);
        recyclerView.setNestedScrollingEnabled(false);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new WordListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        scrollingTimes.add(ContextDataRetriever.addTimeStamp());
                        break;
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        settlingTimes.add(ContextDataRetriever.addTimeStamp());
                        break;
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });


        setTaskInstructions();
        buildDialog();
        setNextButtonStandardBehaviour();


        startButton.setOnClickListener(v -> isTTSinitialized());
        sttButton.setOnClickListener(view -> callSTT());



        providedTask = true;


        displayHelpAtBeginning = bundle.getBoolean("display_help");

        array = new String[0];


        return mainView;
    }


    /**
     * Start the task if the Text-to-Speech is loaded, otherwise it will display a pop-up
     * until it finish loading.
     */
    private void isTTSinitialized() {
        if (!TextToSpeechLocal.isInitialized()) {
            setProgressDialog();
        } else {
            startTask();
        }

    }

    /**
     * Sets and displays the progress dialog.
     */
    private void setProgressDialog() {
        int llPadding = 30;
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(llPadding, llPadding, llPadding, llPadding);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, llPadding, 0);
        progressBar.setLayoutParams(llParam);

        llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        TextView tvText = new TextView(context);
        tvText.setText(R.string.loading);
        tvText.setTextColor(Color.parseColor("#000000"));
        tvText.setTextSize(20);
        tvText.setLayoutParams(llParam);

        ll.addView(progressBar);
        ll.addView(tvText);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setView(ll);

        progressDialog = builder.create();
        progressDialog.show();
        Window window = progressDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(progressDialog.getWindow().getAttributes());
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            progressDialog.getWindow().setAttributes(layoutParams);
        }
    }


    /**
     * Sets the next button to a non-standard behaviour, where a loop must be done.
     */
    private void setNextButtonLoopTask() {
        rightButton.setOnClickListener(v -> {
            hideInputs();
            clearInputs();
            showCountdownAgain();
            clearRecyclerView();
            startTask();
        });
    }


    /**
     * Starts the task
     */
    private void startTask() {

        try {
            callBack.getJsonContextEvents().addField(ContextDataRetriever.GenericTimeStartTask, ContextDataRetriever.addTimeStamp());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (taskType != RECALL && taskType != ATTENTION_SUBTRACTION && taskType != ABSTRACTION && taskType != ORIENTATION) {
            new CountDownTimer(context.getResources().getInteger(R.integer.default_time), context.getResources().getInteger(R.integer.one_seg_millis)) {

                public void onTick(long millisUntilFinished) {
                    String display = Long.toString(millisUntilFinished / 1000, 10);
                    countdownText.setText(display);
                    rightButton.setClickable(false);
                }

                public void onFinish() {
                    countdownText.setVisibility(View.GONE);
                    rightButton.setClickable(true);
                    Objects.requireNonNull(bundle);
                    switch (taskType) {
                        case MEMORY:
                            memorization(bundle.getStringArray("words"), bundle.getInt("times"));
                            break;
                        case ATTENTION_NUMBERS:
                            if(firstDone){
                                repeatBackwards(bundle.getStringArray("numbers_backward"));
                            }
                            else{
                                repeat(bundle.getStringArray("numbers_forward"));
                            }
                            onlyNumbersInputAccepted = true;
                            break;
                        case ATTENTION_LETTERS:
                            tapLetter(bundle.getStringArray("letters"));
                            break;
                        case LANGUAGE:
                            repeatPhrase(Objects.requireNonNull(bundle.getStringArray("phrases")));
                            break;
                        case FLUENCY:
                            fluency();
                            break;

                        default:
                            throw new RuntimeException("INVALID TASKTYPE");
                    }

                }
            }.start();
        }
        else{
            switch (taskType){
                case RECALL:
                    recall(bundle.getStringArray("words"));
                    break;
                case ORIENTATION:
                    orientation(Objects.requireNonNull(bundle.getStringArray("questions")));
                    break;
                case ATTENTION_SUBTRACTION:
                    subtractions(bundle.getInt("minuend"), bundle.getInt("subtracting"), bundle.getInt("times"));
                    onlyNumbersInputAccepted = true;
                    break;
                case ABSTRACTION:
                    abstraction(bundle.getStringArray("words"), bundle.getStringArray("answer"));
                    break;
                default:
                    throw new RuntimeException("INVALID TASKTYPE");
            }
        }

        startButton.setVisibility(View.GONE);
        providedTask = true;
        timesCompleted++;
    }


    //-------------------TASKS-------------------//

    private void memorization(String[] words, int times) {
        array = words;
        index = 0;
        length = times;
        enumeration();
        if (times > 1) {
            bannerText.setText(R.string.memory_instructions_2);
            setNextButtonLoopTask();
        } else {
            setNextButtonStandardBehaviour();
        }
        firstInput.requestFocus();
        firstInput.setOnEditorActionListener((v, actionId, event) -> handleSubmitKeyboardButton(actionId));
        enableWordList();
        if(timesCompleted == times){
            bannerText.setText(R.string.memory_instructions_last);
            setNextButtonStandardBehaviour();
        }
        addTextWatcherToInput();

    }

    private void repeat(String[] numbers) {
        array = numbers;
        index = 0;
        placeFirstInput();
        changeInputFilterAndType();
        setVariousInputs();
        enumeration();
        View.OnClickListener clickListener = v -> {
            firstInput.requestFocus();
            bannerText.setText(R.string.backwards_instructions);
            hideInputs();
            callBack.hideKeyboard(parent);
            try {
                saveResults();
            } catch (JSONException e) {
                ErrorHandler.displayError(e.getMessage());
            }

            firstDone = true;
            addSubmitTime();

            startButton.setVisibility(View.VISIBLE);
            startButton.setOnClickListener(v1 -> {
                showCountdownAgain();
                startTask();
            });
        };

        submitAnswerButton.setOnClickListener(clickListener);
        rightButton.setOnClickListener(clickListener);
    }

    private void repeatBackwards(String[] numbers) {
        array = numbers;
        index = 0;
        clearInputs();
        clearAnswers();
        hideInputs();
        placeFirstInput();
        setVariousInputs();
        enumeration();
        View.OnClickListener clickListener = v -> {
            addSubmitTime();
            hideInputs();
            taskIsEnded();
        };
        submitAnswerButton.setOnClickListener(clickListener);
        rightButton.setOnClickListener(clickListener);
    }



    private void tapLetter(String[] words) {
        array = words;
        index = 0;
        lastIndex = 0;
        playableArea.setClickable(true);
        playableArea.setFocusable(true);
        playableArea.setOnClickListener(v -> {
            if(index > 0 && lastIndex != index) {
                answers.add(array[index - 1]);
                Toast.makeText(context, R.string.click_done, Toast.LENGTH_SHORT).show();
                lastIndex = index;
                clickingTimes.add(ContextDataRetriever.addTimeStamp());
            }

        });

        enumeration();
    }

    private void subtractions(int startingNumber, final int subtracting, final int times) {
        index = 0;
        String displaySubtraction = startingNumber + " - " + subtracting;
        additionalTaskText.setText(displaySubtraction);
        firstInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        firstInput.setOnEditorActionListener((v, actionId, event) -> handleSubmitKeyboardButton(actionId));
        submitAnswerButton.setOnClickListener(v -> {
            if (!firstInput.getText().toString().isEmpty()) {
                addSubmitTime();
                answers.add(firstInput.getText().toString());
                if (index == length) {
                    submitAnswerButton.setVisibility(View.GONE);
                    hideInputs();
                    taskIsEnded();
                    checkSubtractions(startingNumber, subtracting);

                } else {
                    String displayAnotherSubtraction = firstInput.getText().toString() + " - " + subtracting;
                    additionalTaskText.setText(displayAnotherSubtraction);
                    index++;
                }
                clearInputs();
            } else
                Toast.makeText(context, R.string.provide_data, Toast.LENGTH_LONG).show();
        });
        length = times;

        showUserInput();
        hideMicro();
        addTextWatcherToInput();
    }

    private void repeatPhrase(String[] phrases) {
        array = phrases;
        firstInput.setOnEditorActionListener((v, actionId, event) -> handleSubmitKeyboardButton(actionId));
        View.OnClickListener clickListener = v -> {
            addSubmitTime();
            answers.add(firstInput.getText().toString());
            clearInputs();
            ++index;
            hideInputs();
            showCountdownAgain();
            if(index >= length){
                taskIsEnded();
            } else{
                speakPhrase(phrases[index]);
            }
        };

        addTextWatcherToInput();

        submitAnswerButton.setOnClickListener(clickListener);
        rightButton.setOnClickListener(clickListener);
        length = phrases.length;

        speakPhrase(phrases[index]);
    }

    private void fluency() {
        firstInput.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        showUserInput();
        firstInput.requestFocus();
        firstInput.setOnEditorActionListener((v, actionId, event) -> handleSubmitKeyboardButton(actionId));
        enableWordList();
        addTextWatcherToInput();
        countDownTask(getResources().getInteger(R.integer.one_minute_millis));
    }

    private void abstraction(String[] words, String[] answers) {
        array = words;
        setExpectedAnswers(answers);
        additionalTaskText.setText(array[index]);
        index++;
        showUserInput();
        firstInput.setOnEditorActionListener((v, actionId, event) -> handleSubmitKeyboardButton(actionId));
        submitAnswerButton.setOnClickListener(v -> {
            addSubmitTime();
            this.answers.add(firstInput.getText().toString());
            if(index >= length){
                hideInputs();
                taskIsEnded();
            }
            else{
                clearInputs();
                additionalTaskText.setText(array[index]);
                index++;
            }
        });

        addTextWatcherToInput();
        length = array.length;

    }


    private void recall(String[] words) {
        array = words;
        showUserInput();
        enableWordList();
        addTextWatcherToInput();
        firstInput.setOnEditorActionListener((v, actionId, event) -> handleSubmitKeyboardButton(actionId));
    }


    private void orientation(String[] questions) {

        array = questions;
        showUserInput();

        submitAnswerButton.setOnClickListener(v -> {
            addSubmitTime();
            answers.add(firstInput.getText().toString());
            clearInputs();
            ++index;
            if(index >= length){
                setNextButtonStandardBehaviour();
                hideInputs();
                taskIsEnded();
            }
            else{
                setNextButtonLoopTask();
                additionalTaskText.setText(array[index]);
            }
        });

        addTextWatcherToInput();
        length = questions.length;
        additionalTaskText.setText(array[index]);
        firstInput.setOnEditorActionListener((v, actionId, event) -> handleSubmitKeyboardButton(actionId));
    }

    //-------------------END TASKS-------------------//

    //-------------------UI-------------------//

    /**
     * Sets a countdown to the task.
     *
     * @param millis the time milliseconds where the countdown will start with.
     */
    private void countDownTask(int millis) {
        handler = new Handler();
        handler.postDelayed(() -> {
            taskEnded = true;
            try {
                callBack.getJsonContextEvents().addField(ContextDataRetriever.GenericTimeEndTask, ContextDataRetriever.addTimeStamp());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            hideInputs();
            hideBanner();
        }, millis);
    }

    /**
     * Hides the microphone button.
     */
    private void hideMicro() {
        sttButtonContainer.setVisibility(View.GONE);
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainLayout);
        constraintSet.connect(R.id.submitButtonContainer, ConstraintSet.RIGHT, R.id.textTaskLayout, ConstraintSet.RIGHT, 8);
        constraintSet.connect(R.id.submitButtonContainer, ConstraintSet.LEFT, R.id.textTaskLayout, ConstraintSet.LEFT, 8);
        constraintSet.applyTo(mainLayout);
        rearrangeSubmitAnswerContainer();
    }

    /**
     * Clears the text in the inputs
     */
    private void clearInputs() {
        clearedByMethod = true;
        firstInput.getText().clear();
        if (variousInputs != null) {
            for (int i = 1; i < variousInputs.size(); ++i) {
                mainLayout.removeView(variousInputs.get(i));
            }
            variousInputs.clear();
        }
    }

    /**
     * Clears the answers array
     */
    private void clearAnswers() {
        answers.clear();
    }

    /**
     * Sets various inputs in the task.
     */
    private void setVariousInputs() {
        int[] numbersId = new int[array.length];
        numbersId[0] = firstInput.getId();
        variousInputs = new ArrayList<>();
        variousInputs.add(firstInput);

        ConstraintSet set = new ConstraintSet();
        set.clone(mainLayout);

        int dimens = getResources().getDimensionPixelSize(R.dimen.input_dimen);
        int portion = mainLayout.getWidth()/array.length;
        int positionX = portion-mainLayout.getWidth()/2;


        for(int i = 1; i < array.length; ++i){
            EditText editText = new EditText(context);
            editText.setId(View.generateViewId());
            editText.setTag(Integer.toString(i));
            editText.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editText.setGravity(Gravity.CENTER_HORIZONTAL);
            editText.setKeyListener(DigitsKeyListener.getInstance("123456789"));
            InputFilter[] inputArray = new InputFilter[1];
            inputArray[0] = new InputFilter.LengthFilter(1);
            editText.setFilters(inputArray);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if(count > 0) {
                        if (variousInputs.size() > index+1) {
                            variousInputs.get(index+1).requestFocus();
                        }
                        addWritingTime();
                        positionFilling.add(index);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            editText.setOnFocusChangeListener((v, hasFocus) -> {
                if(hasFocus){
                    index = Integer.parseInt(v.getTag().toString());
                }
            });

            if (i == array.length - 1) {
                editText.setOnEditorActionListener((v, actionId, event) -> handleSubmitKeyboardButton(actionId));
            }


            set.connect(editText.getId(), ConstraintSet.LEFT, numbersId[i - 1], ConstraintSet.RIGHT, 8);
            set.connect(editText.getId(), ConstraintSet.TOP, numbersId[i - 1], ConstraintSet.TOP, 0);
            set.constrainHeight(editText.getId(), dimens);
            set.constrainWidth(editText.getId(), dimens);
            set.setTranslationX(editText.getId(), positionX);
            set.setVisibility(editText.getId(), ConstraintSet.INVISIBLE);
            mainLayout.addView(editText);
            set.applyTo(mainLayout);

            numbersId[i] = editText.getId();
            variousInputs.add(editText);
        }
    }

    /**
     * Place the first input in the task, the others inputs will be next to it.
     */
    private void placeFirstInput() {
        ConstraintSet set = new ConstraintSet();
        set.clone(mainLayout);

        int dimens = getResources().getDimensionPixelSize(R.dimen.input_dimen);
        set.constrainHeight(firstInput.getId(), dimens);
        set.constrainWidth(firstInput.getId(), dimens);

        int portion = mainLayout.getWidth() / array.length;
        int positionX = portion - mainLayout.getWidth() / 2;
        set.setTranslationX(firstInput.getId(), positionX);
        set.applyTo(mainLayout);

        firstInput.setScrollContainer(true);
        firstInput.setTag("0");

        firstInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count > 0) {
                    index=1;
                    if (variousInputs.size() > index) {
                        variousInputs.get(index).requestFocus();
                        addWritingTime();
                        positionFilling.add(index);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        firstInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                index = Integer.parseInt(v.getTag().toString());
            }
        });

    }

    /**
     * Change the input filter and type to numbers
     */
    private void changeInputFilterAndType() {
        firstInput.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        firstInput.setKeyListener(DigitsKeyListener.getInstance("123456789"));
        InputFilter[] inputArray = new InputFilter[1];
        inputArray[0] = new InputFilter.LengthFilter(1);
        firstInput.setFilters(inputArray);
    }

    /**
     * Sets the text in the top banner.
     */
    private void setTaskInstructions() {
        switch (taskType) {
            case MEMORY:
                bannerText.setText(R.string.memory_instructions);
                break;
            case ATTENTION_NUMBERS:
                bannerText.setText(R.string.numbers_instructions);
                break;
            case ATTENTION_LETTERS:
                bannerText.setText(context.getResources().getString(R.string.letters_instructions, bundle.getString("target_letter")));
                break;
            case ATTENTION_SUBTRACTION:
                bannerText.setText(context.getResources().getString(R.string.substraction_instructions, bundle.getInt("minuend"), bundle.getInt("subtracting")));
                break;
            case LANGUAGE:
                bannerText.setText(R.string.language_instructions);
                break;
            case FLUENCY:
                bannerText.setText(context.getResources().getString(R.string.fluency_instructions, bundle.getString("target_letter")));
                break;
            case ABSTRACTION:
                bannerText.setText(R.string.abstraction_instruction);
                break;
            case RECALL:
                bannerText.setText(R.string.recall_instructions);
                break;
            case ORIENTATION:
                bannerText.setText(R.string.orientation_instructions);
                break;
            default:
                throw new RuntimeException("INVALID TASKTYPE");
        }
    }

    /**
     * Shows the inputs
     */
    private void showUserInput() {
        additionalTaskText.setVisibility(View.VISIBLE);
        firstInput.setVisibility(View.VISIBLE);
        firstInput.requestFocus();
        submitAnswerContainer.setVisibility(View.VISIBLE);
        sttButtonContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        if (variousInputs != null && variousInputs.size() > 0) {
            for (EditText editText : variousInputs) {
                editText.setVisibility(View.VISIBLE);
            }
        }
        callBack.showKeyboard(parent);
    }

    /**
     * Hide the inputs
     */
    private void hideInputs() {
        additionalTaskText.setVisibility(View.INVISIBLE);
        firstInput.setVisibility(View.INVISIBLE);
        submitAnswerContainer.setVisibility(View.INVISIBLE);
        sttButtonContainer.setVisibility(View.INVISIBLE);
        recyclerView.setVisibility(View.VISIBLE);
        if (variousInputs != null && variousInputs.size() > 0) {
            for (EditText editText : variousInputs) {
                editText.setVisibility(View.INVISIBLE);
            }
        }
        callBack.hideKeyboard(parent);

    }

    /**
     * Displays the countdown task again.
     */
    private void showCountdownAgain() {
        countdownText.setVisibility(View.VISIBLE);
    }

    /**
     * Clears the recycler view.
     */
    private void clearRecyclerView() {
        adapter.removeAllWords();
    }

    /**
     * Enables the list of words in the task.
     */
    private void enableWordList() {
        TextView submitButtonLabel = mainView.findViewById(R.id.submitButtonLabel);
        submitButtonLabel.setText(R.string.add_word);
        submitAnswerButton.setOnClickListener(v -> {
            if (!firstInput.getText().toString().isEmpty()) {
                addSubmitTime();
                adapter.addWord(firstInput.getText().toString());
                recyclerView.scrollToPosition(0);
            } else
                Toast.makeText(context, R.string.provide_data, Toast.LENGTH_LONG).show();

            clearInputs();
        });
    }

    /**
     * Changes the submit button icon.
     */
    private void changeSubmitButton() {
        submitAnswerButton.setImageResource(R.drawable.ic_check_black_24dp);
        TextView submitButtonLabel = mainView.findViewById(R.id.submitButtonLabel);
        submitButtonLabel.setText(R.string.save);
    }

    /**
     * Restores the submit button icon.
     */
    private void restoreSubmitButton() {
        submitAnswerButton.setImageResource(R.drawable.add_word_24dp);
        TextView submitButtonLabel = mainView.findViewById(R.id.submitButtonLabel);
        submitButtonLabel.setText(R.string.add_word);
    }

    /**
     * Displays the edit elements in the word layout.
     */
    private void showEditElements() {
        if (taskEnded) {
            if (firstInput.getVisibility() != View.VISIBLE) {
                firstInput.setVisibility(View.VISIBLE);
                submitAnswerContainer.setVisibility(View.VISIBLE);
                rearrangeSubmitAnswerContainer();
            } else {
                firstInput.setVisibility(View.GONE);
                submitAnswerContainer.setVisibility(View.GONE);
            }
        }


    }

    /**
     * Rearrange the UI to be clearer and more responsive.
     */
    private void rearrangeSubmitAnswerContainer() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mainLayout);
        constraintSet.connect(submitAnswerContainer.getId(), ConstraintSet.START, mainLayout.getId(), ConstraintSet.START, (int) getResources().getDimension(R.dimen.default_margin));
        constraintSet.connect(submitAnswerContainer.getId(), ConstraintSet.END, mainLayout.getId(), ConstraintSet.END, (int) getResources().getDimension(R.dimen.default_margin));
        constraintSet.connect(submitAnswerContainer.getId(), ConstraintSet.TOP, firstInput.getId(), ConstraintSet.BOTTOM, (int) getResources().getDimension(R.dimen.margin_medium));
        constraintSet.applyTo(mainLayout);
    }


    //-------------------END UI-------------------//

    //-------------------TTS-------------------//

    /**
     * Overrides {@link TTSHandler#startTTS()}
     */
    @Override
    public void startTTS() {
        startButton.setClickable(true);
    }

    /**
     * Overrides {@link TTSHandler#TTSEnded()}
     */
    @Override
    public void TTSEnded() {
        final Handler handler = new Handler(Looper.getMainLooper());
        if (taskType != ATTENTION_LETTERS) {
            handler.post(this::showUserInput);
            switch (CognimobilePreferences.getConfig(context)) {
                case DEFAULT:
                    if (taskType == ATTENTION_NUMBERS || taskType == ATTENTION_SUBTRACTION) {
                        handler.post(this::hideMicro);
                    }
                    break;
                case ONLY_TEXT:
                    handler.post(this::hideMicro);
                    break;
                case ONLY_LANGUAGE:
                    handler.post(this::cantEdit);
                    break;
            }

        } else {
            taskEnded = true;
            try {
                callBack.getJsonContextEvents().addField(ContextDataRetriever.GenericTimeEndTask, ContextDataRetriever.addTimeStamp());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            handler.postDelayed(this::taskIsEnded, context.getResources().getInteger(R.integer.one_seg_millis));
        }

    }

    /**
     * It does not allow the input to be clicked, to force the user to use only microphone.
     */
    private void cantEdit() {
        firstInput.setEnabled(false);
        firstInput.setKeyListener(null);
    }

    /**
     * Text-to-Speech enumeration.
     */
    private void enumeration() {
        TextToSpeechLocal.enumerate(array);
    }

    /**
     * Text-to-Speech speaks the given phrase.
     *
     * @param phrase to be spoken
     */
    private void speakPhrase(String phrase) {
        TextToSpeechLocal.readOutLoud(phrase);
    }

    @Override
    public void onPause() {
        TextToSpeechLocal.stop();
        super.onPause();
    }

    @Override
    public void onStart() {
        TextToSpeechLocal.instantiate(context, this, callBack.getLanguage());
        super.onStart();
    }

    @Override
    public void onStop() {
        TextToSpeechLocal.stop();
        TextToSpeechLocal.clear();
        super.onStop();
    }

    /**
     * onActivityResult method allows to catch information from a another activity.
     * This method retrieves the text from the Speech-to-Text function.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     *                    <p>
     *                    It catches the link url to be consumed by AWARE.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == TextToSpeechLocal.STT_CODE) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> results = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null) {
                    String answer = null;
                    for (Object result : results) {
                        String option = result.toString();
                        if (onlyNumbersInputAccepted) {
                            if (answer == null || answer.isEmpty())
                                answer = option.replaceAll("\\D", "");
                        } else {
                            if (answer == null || answer.isEmpty())
                                answer = option.replaceAll("\\d", "");
                        }

                    }

                    if(answer !=null) {
                        if (variousInputs != null && variousInputs.size() > 0) {
                            if(index > variousInputs.size() ) index = 0;
                            String[] tokens = answer.split("(?!^)");
                            for (int k = 0; k < tokens.length && index < variousInputs.size(); ++index, ++k) {
                                if (tokens[k].isEmpty() || tokens[k].equals("")) {
                                    ++k;
                                } else {
                                    variousInputs.get(index).setText(tokens[k]);
                                }
                            }
                        } else {
                            firstInput.setText(answer);
                        }
                    }

                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Calls Speech-to-Text functionality
     */
    private void callSTT() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            startActivityForResult(intent, TextToSpeechLocal.STT_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(context,
                    "Sorry your device not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    //-------------------END TTS-------------------//

    //-------------------GETTERS-------------------//

    /**
     * Gets the playable area of the task
     *
     * @return the playable area of the task
     */
    RelativeLayout getPlayableArea() {
        return playableArea;
    }

    /**
     * Gets the main layout
     *
     * @return the main layout
     */
    public ConstraintLayout getMainLayout() {
        return mainLayout;
    }

    //-------------------END GETTERS-------------------//

    //-------------------INTERFACES-------------------//

    /**
     * Overrides {@link Task#saveResults()}
     *
     * @throws JSONException if the json was not able to handle the information.
     */
    @Override
    void saveResults() throws JSONException {
        callBack.getJsonAnswerWrapper().addField("task_type", taskType);
        switch (taskType) {
            case ATTENTION_NUMBERS:
                if (variousInputs != null && variousInputs.size() > 0) {
                    for (EditText editText : variousInputs) {
                        answers.add(editText.getText().toString());
                    }
                    if (firstDone) {
                        callBack.getJsonAnswerWrapper().addArrayList("answer_backwards", answers);
                        callBack.getJsonAnswerWrapper().addStringArray("expected_answer_backwards", array);
                        callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificAttentionNumbersItemPositionBackwards, ContextDataRetriever.retrieveInformationFromIntegerArrayList(positionFilling));
                        callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificAttentionNumbersStartWriting, ContextDataRetriever.retrieveInformationFromLongArrayList(startWritingTimes));
                        callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificAttentionNumbersSubmitAnswer, ContextDataRetriever.retrieveInformationFromLongArrayList(submitAnswerTimes));
                    } else {
                        callBack.getJsonAnswerWrapper().addArrayList("answer", answers);
                        callBack.getJsonAnswerWrapper().addStringArray("expected_answer", array);
                        callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificAttentionNumbersItemPosition, ContextDataRetriever.retrieveInformationFromIntegerArrayList(positionFilling));
                        positionFilling.clear();
                    }

                }
                setScoring();
                break;

            case FLUENCY:
                answers = adapter.getAllWords();
                Collections.reverse(answers);
                callBack.getJsonAnswerWrapper().addArrayList("answer", answers);
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificFluencyScrollingList, ContextDataRetriever.retrieveInformationFromLongArrayList(scrollingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificFluencySettlingList, ContextDataRetriever.retrieveInformationFromLongArrayList(settlingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificFluencyCharacterChange, ContextDataRetriever.retrieveInformationFromStringArrayList(characterChange));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificFluencyStartWriting, ContextDataRetriever.retrieveInformationFromLongArrayList(startWritingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificFluencySubmitAnswer, ContextDataRetriever.retrieveInformationFromLongArrayList(submitAnswerTimes));
                setScoring();
                break;
            case RECALL:
                answers = adapter.getAllWords();
                Collections.reverse(answers);
                callBack.getJsonAnswerWrapper().addArrayList("answer", answers);
                callBack.getJsonAnswerWrapper().addStringArray("expected_answer", array);
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificRecallScrollingList, ContextDataRetriever.retrieveInformationFromLongArrayList(scrollingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificRecallSettlingList, ContextDataRetriever.retrieveInformationFromLongArrayList(settlingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificRecallCharacterChange, ContextDataRetriever.retrieveInformationFromStringArrayList(characterChange));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificRecallStartWriting, ContextDataRetriever.retrieveInformationFromLongArrayList(startWritingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificRecallSubmitAnswer, ContextDataRetriever.retrieveInformationFromLongArrayList(submitAnswerTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificRecallNumbersOfWords, array.length);
                setScoring();
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificRecallNumbersOfCorrectWords, score);
                break;

            case MEMORY:
                answers = adapter.getAllWords();
                startWritingTimes.remove(startWritingTimes.size() - 1);
                callBack.getJsonAnswerWrapper().addArrayList("answer", answers);
                callBack.getJsonAnswerWrapper().addStringArray("expected_answer", array);
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificMemoryScrollingList, ContextDataRetriever.retrieveInformationFromLongArrayList(scrollingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificMemorySettlingList, ContextDataRetriever.retrieveInformationFromLongArrayList(settlingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificMemoryCharacterChange, ContextDataRetriever.retrieveInformationFromStringArrayList(characterChange));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificMemoryStartWriting, ContextDataRetriever.retrieveInformationFromLongArrayList(startWritingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificMemorySubmitAnswer, ContextDataRetriever.retrieveInformationFromLongArrayList(submitAnswerTimes));
                break;
            case ATTENTION_LETTERS:
                callBack.getJsonAnswerWrapper().addArrayList("answer", answers);
                callBack.getJsonAnswerWrapper().addStringArray("letters", array);
                callBack.getJsonAnswerWrapper().addField("occurrences", getLetterOccurrences());
                callBack.getJsonAnswerWrapper().addField("target_letter", bundle.getString("target_letter"));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificAttentionLettersSoundTimes, ContextDataRetriever.retrieveInformationFromLongArrayList(soundTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificAttentionLettersTimeToAnswer, ContextDataRetriever.retrieveInformationFromLongArrayList(clickingTimes));
                setScoring();
                break;
            case ATTENTION_SUBTRACTION:
                callBack.getJsonAnswerWrapper().addArrayList("answer", answers);
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificSubtractionCharacterChange, ContextDataRetriever.retrieveInformationFromStringArrayList(characterChange));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificSubtractionStartWriting, ContextDataRetriever.retrieveInformationFromLongArrayList(startWritingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificSubtractionSubmitAnswer, ContextDataRetriever.retrieveInformationFromLongArrayList(submitAnswerTimes));
                setScoring();
                break;
            case LANGUAGE:
                callBack.getJsonAnswerWrapper().addArrayList("answer", answers);
                callBack.getJsonAnswerWrapper().addStringArray("expected_answer", array);
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificSRCharacterChange, ContextDataRetriever.retrieveInformationFromStringArrayList(characterChange));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificSRStartWriting, ContextDataRetriever.retrieveInformationFromLongArrayList(startWritingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificSRSubmitAnswer, ContextDataRetriever.retrieveInformationFromLongArrayList(submitAnswerTimes));
                setScoring();
                break;
            case ABSTRACTION:
                callBack.getJsonAnswerWrapper().addArrayList("answer", answers);
                callBack.getJsonAnswerWrapper().addStringArray("words", array);
                callBack.getJsonAnswerWrapper().addArrayList("expected_answer", expectedAnswers);
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificAbstractionCharacterChange, ContextDataRetriever.retrieveInformationFromStringArrayList(characterChange));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificAbstractionStartWriting, ContextDataRetriever.retrieveInformationFromLongArrayList(startWritingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificAbstractionSubmitAnswer, ContextDataRetriever.retrieveInformationFromLongArrayList(submitAnswerTimes));
                setScoring();
                break;
            case ORIENTATION:
                callBack.getJsonAnswerWrapper().addArrayList("answer", answers);
                callBack.getJsonAnswerWrapper().addStringArray("questions", array);
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificOrientationCharacterChange, ContextDataRetriever.retrieveInformationFromStringArrayList(characterChange));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificOrientationStartWriting, ContextDataRetriever.retrieveInformationFromLongArrayList(startWritingTimes));
                callBack.getJsonContextEvents().addField(ContextDataRetriever.SpecificOrientationSubmitAnswer, ContextDataRetriever.retrieveInformationFromLongArrayList(submitAnswerTimes));
                setScoring();
                break;

        }


    }

    /**
     * Get letters occurrences to put some information context in the json results.
     *
     * @return number of letters occurrences
     */
    private int getLetterOccurrences() {
        Matcher matcher
                = Pattern.compile(Objects.requireNonNull(bundle.getString("target_letter")))
                .matcher(Arrays.toString(array));
        int res = 0;

        while (matcher.find()) {
            res++;
        }
        return res;
    }

    /**
     * Overrides {@link Task#setScoring()} ()}
     */
    @Override
    void setScoring() {
        switch (taskType) {
            case ATTENTION_NUMBERS:
                if (firstDone) {
                    checkReverseAnswerArray();
                    addScoreToJson();
                } else {
                    checkAnswerArray();
                }
                break;

            case ATTENTION_LETTERS:
                checkLettersPressed();
                break;

            case LANGUAGE:
                checkPhrases();
                break;

            case FLUENCY:
                checkFluency();
                break;

            case ABSTRACTION:
                checkAbstraction();
                break;

            case RECALL:
                checkRecall();
                break;

            case ORIENTATION:
                checkOrientation();
                break;

        }

        if (taskType != ATTENTION_NUMBERS) {
            addScoreToJson();
        }


    }


    /**
     * Overrides {@link TextTaskCallback#editWord(String)} ()}
     *
     * @param word to be edited
     */
    @Override
    public void editWord(String word) {
        firstInput.setText(word);
        if (taskType == Task.FLUENCY)
            showEditElements();
        changeSubmitButton();
        submitAnswerButton.setOnClickListener(v -> {
            addSubmitTime();
            adapter.editWord(word, firstInput.getText().toString());
            clearInputs();
            if (taskType == Task.FLUENCY) {
                showEditElements();
            }
            enableWordList();
            restoreSubmitButton();


        });
    }

    /**
     * Overrides {@link TTSHandler#setIndex(int)}
     *
     * @param index to be set on the fragment.
     */
    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Overrides {@link TTSHandler#TTSisInitialized()}
     */
    @Override
    public void TTSisInitialized() {
        if (progressDialog != null)
            progressDialog.dismiss();
        if (startButton.getVisibility() != View.VISIBLE)
            startTask();
    }

    /**
     * Overrides {@link TTSHandler#registerTimeStamp()}
     */
    @Override
    public void registerTimeStamp() {
        soundTimes.add(ContextDataRetriever.addTimeStamp());
    }

    //-------------------END INTERFACES-------------------//


    //-------------------CHECKERS-------------------------//

    /**
     * Sets the expected answers if the task needs it.
     *
     * @param answers expected answers.
     */
    private void setExpectedAnswers(String[] answers) {
        expectedAnswers = new ArrayList<>(Arrays.asList(answers));
    }


    /*
     * METHODS TO CHECK THE SCORE OF THE TASKS.
     */

    private void checkOrientation() {
        score = answers.size() > 0 ? score + 1 : score;
    }

    private void checkRecall() {
        checkPhrases();
    }

    private void checkAbstraction() {
        int errors = 0;

        /*
            Only the last two item pairs are scored. Give 1 point to each item pair correctly
            answered.
         */

        for(int i = 1; i < answers.size(); ++i) {
            String expectedAnswer = expectedAnswers.get(i);
            if (expectedAnswer.contains(answers.get(i))) {
                ++errors;
            }
        }


        score = answers.size() - errors;
    }

    private void checkFluency() {
        int minimumWords = bundle.getInt("number_words");
        answers = adapter.getAllWords();

        score = 0;

        if (answers.size() >= minimumWords) {
            int errors = callBack.checkTypos(answers);
            if (answers.size() - errors > minimumWords) {
                score = 1;
            }
        }



    }

    private void checkPhrases() {
        int errors = 0;

        for(int i = 0; i < answers.size(); ++i) {
            if (!answers.get(i).equalsIgnoreCase(array[i])) {
                ++errors;
            }
        }
        /*
            Allocate 1 point for each sentence correctly repeated. Repetition must be exact.
         */

        score = answers.size() - errors;
    }

    private void addScoreToJson() {
        try {
            callBack.getJsonAnswerWrapper().addField("score",score);
        } catch (JSONException e) {
            ErrorHandler.displayError(e.getMessage());
        }
    }

    private void checkAnswerArray() {

        boolean goOn = true;

        for(int i = 0; i < answers.size() && goOn; ++i) {
            if (!array[i].equals(answers.get(i))) {
                goOn = false;
            }
        }
        //if onePoint = true -> 1, otherwise -> 0
        score = goOn ? score + 1 : score;
    }

    private void checkReverseAnswerArray() {
        boolean goOn = true;

        int lastElement = answers.size() - 1;

        for (int i = 0; i < answers.size() && goOn; ++i) {
            if (!array[lastElement - i].equals(answers.get(i))) {
                goOn = false;
            }
        }
        //if onePoint = true -> 1, otherwise -> 0
        score = goOn ? score + 1 : score;
    }

    private void checkLettersPressed(){
        boolean goOn = true;
        int errors = 0;

        if (answers.size() > 0) {
            for (int i = 1; i < answers.size() && goOn; ++i) {
                if (!answers.get(i - 1).equalsIgnoreCase(answers.get(i))) {
                    errors++;
                    if (errors > 1) {
                        goOn = false;
                    }
                }
            }
        } else {
            goOn = false;
        }
        //if onePoint = true -> 1, otherwise -> 0
        score = goOn ? score + 1 : score;
    }

    private void checkSubtractions(int firstMinuend, int subtrahend) {

        int errors = 0;
        int expectedResult = firstMinuend - subtrahend;


        for(int i = 0; i < answers.size(); ++i) {
            int result = Integer.parseInt(answers.get(i));
            if(expectedResult != result){
                ++errors;
            }
            expectedResult = result - subtrahend;
        }

        /*
            Give no (0) points for no correct subtractions,
            1 point for one correction subtraction,
            2 points for two-to-three correct subtractions,
            and 3 points if the participant successfully makes four or five correct subtraction.
         */

        switch (errors){
            case 0:
            case 1:
                score = 3;
                break;
            case 2:
            case 3:
                score = 2;
                break;
            case 4:
                score = 1;
                break;
            default:
                score = 0;
                break;
        }

        try {
            callBack.getJsonAnswerWrapper().addField("errors", errors);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //-------------------END CHECKERS-------------------------//

}
