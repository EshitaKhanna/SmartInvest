package com.example.investmentplatform.entity;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import com.example.investmentplatform.enums.InvestmentGoal;
import com.example.investmentplatform.enums.RiskTolerance;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "main_investment_goal", nullable = false)
    private InvestmentGoal mainInvestmentGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tolerance", nullable = false)
    private RiskTolerance riskTolerance;

    @Column(name = "annual_income", nullable = false)
    private String annualIncome;

    @Column(name = "investment_amount", nullable = false)
    private Double investmentAmount;

    // UserDetails interface methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Getters and Setters (complementing what @Data doesn't handle perfectly)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public InvestmentGoal getMainInvestmentGoal() {
        return mainInvestmentGoal;
    }

    public void setMainInvestmentGoal(InvestmentGoal mainInvestmentGoal) {
        this.mainInvestmentGoal = mainInvestmentGoal;
    }

    public RiskTolerance getRiskTolerance() {
        return riskTolerance;
    }

    public void setRiskTolerance(RiskTolerance riskTolerance) {
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

    public void setInvestmentAmount(Double investmentAmount) {
        this.investmentAmount = investmentAmount;
    }
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", dob=" + dob +
                ", mainInvestmentGoal=" + mainInvestmentGoal +
                ", riskTolerance=" + riskTolerance +
                ", annualIncome='" + annualIncome  +
                ", investmentAmount='" + investmentAmount + '\'' +
                '}';
    }

}