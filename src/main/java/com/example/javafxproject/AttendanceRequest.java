package com.example.javafxproject;

public class AttendanceRequest {
    public Long id;
    public String attendanceMarkedAt;
    public String orgId;

    public AttendanceRequest() {
    }

    public AttendanceRequest(Long id, String attendanceMarkedAt) {
        this.id = id;
        this.attendanceMarkedAt = attendanceMarkedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAttendanceMarkedAt() {
        return attendanceMarkedAt;
    }

    public void setAttendanceMarkedAt(String attendanceMarkedAt) {
        this.attendanceMarkedAt = attendanceMarkedAt;
    }

    public void setOrgId(String organizationId) {
    }

}
