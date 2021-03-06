package com.peiload.ridecare.unitTest.user.model;

import com.peiload.ridecare.user.dto.UserEditDto;
import com.peiload.ridecare.user.dto.UserSetDto;
import com.peiload.ridecare.user.dto.UserShowDto;
import com.peiload.ridecare.user.model.User;

import java.util.Arrays;
import java.util.List;

public class TestUser {

    public static User getUser1(){
        return User.builder()
                .id(1)
                .email("testUser1@email.com")
                .companyName("Company Test1")
                .password("test1")
                .build();
    }

    public static User getUser2(){
        return User.builder()
                .id(2)
                .email("testUser2@email.com")
                .companyName("Company Test2")
                .password("test2")
                .build();
    }

    public static UserSetDto getUserSetDto1() {
        return UserSetDto.builder()
                .email("testUser1@email.com")
                .companyName("Company Test1")
                .password("test1")
                .build();
    }

    public static UserSetDto getUserSetDto2() {
        return UserSetDto.builder()
                .email("testUser2@email.com")
                .companyName("Company Test2")
                .password("test2")
                .build();
    }

    public static UserEditDto getUserEditDto() {
        return UserEditDto.builder()
                .email("testUser22@email.com")
                .companyName("Company Test22")
                .build();
    }



    public static List<User> getUsersList(){
        return Arrays.asList(getUser1(), getUser2());
    }
}
