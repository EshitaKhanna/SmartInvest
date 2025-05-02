package com.example.investmentplatform.dto;

import java.time.LocalDate;

public class AuthenticationRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String mainInvestmentGoal;
    private String riskTolerance;
    private String annualIncome;
    private Double investmentAmount;


    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getMainInvestmentGoal() {
        return mainInvestmentGoal;
    }

    public void setMainInvestmentGoal(String mainInvestmentGoal) {
        this.mainInvestmentGoal = mainInvestmentGoal;
    }

    public String getRiskTolerance() {
        return riskTolerance;
    }

    public void setRiskTolerance(String riskTolerance) {
        this.riskTolerance = riskTolerance;
    }

    public String getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(String annualIncome) {
        this.annualIncome = annualIncome;
    }

    public Double getInvestmentAmount(){
        return investmentAmount;
    }

    public void setInvestmentAmount(Double investmentAmount){
        this.investmentAmount = investmentAmount;
    }
}