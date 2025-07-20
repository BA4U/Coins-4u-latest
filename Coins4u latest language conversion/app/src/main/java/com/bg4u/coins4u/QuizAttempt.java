package com.bg4u.coins4u;

public class QuizAttempt {
    private String questionId;
    private String chosenAnswer;
    private boolean isCorrect;

    public QuizAttempt() {
        // Firestore needs a public no-argument constructor
    }

    public QuizAttempt(String questionId, String chosenAnswer, boolean isCorrect) {
        this.questionId = questionId;
        this.chosenAnswer = chosenAnswer;
        this.isCorrect = isCorrect;
    }

    // Getters and setters
    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getChosenAnswer() {
        return chosenAnswer;
    }

    public void setChosenAnswer(String chosenAnswer) {
        this.chosenAnswer = chosenAnswer;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }
}
