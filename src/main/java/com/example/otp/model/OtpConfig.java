package com.example.otp.model;

public class OtpConfig {
    private int id;
    private int codeLifeSeconds;
    private int codeLength;

    public OtpConfig() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCodeLifeSeconds() { return codeLifeSeconds; }
    public void setCodeLifeSeconds(int codeLifeSeconds) { this.codeLifeSeconds = codeLifeSeconds; }
    public int getCodeLength() { return codeLength; }
    public void setCodeLength(int codeLength) { this.codeLength = codeLength; }
}