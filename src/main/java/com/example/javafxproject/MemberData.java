package com.example.javafxproject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MemberData {

    private Long id;
    private String name;
    private String phoneNumber;
    private String attendanceMarkedAt;
    private String attendance;
    private String actions;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm:ss");

    public String getFormattedLocalDate() {
        if (attendanceMarkedAt != null) {
            LocalDateTime dateTime = LocalDateTime.parse(attendanceMarkedAt);
            return dateTime.format(formatter);
        } else {
            return "";
        }
    }

    public MemberData(Long id, String name, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.actions = "";
    }

    public MemberData() {
        this.actions = "";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long memberId) {
        this.id = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAttendanceMarkedAt() {
        return attendanceMarkedAt;
    }

    public void setAttendanceMarkedAt(String time) {
        this.attendanceMarkedAt = time;
    }

    public String getAttendance() {
        return attendance;
    }

    public void setAttendance(String attendance) {
        this.attendance = attendance;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public Long getMemberId() {
        return id;
    }

    public String getMemberName() {
        return name;
    }

    public String getPhone() {
        return phoneNumber;
    }

    public void setMemberName(String memberName) {
        this.name = memberName;
    }

}