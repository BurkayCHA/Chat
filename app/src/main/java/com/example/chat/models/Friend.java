package com.example.chat.models;

import java.io.Serializable;

public class Friend implements Serializable {
       private String idChatRoom;
       public String namee,imagee,phonee;

    public Friend() {
    }

    public Friend(String idChatRoom, String namee, String imagee, String phonee) {
        this.idChatRoom = idChatRoom;
        this.namee = namee;
        this.imagee = imagee;
        this.phonee = phonee;
    }

    public String getNamee() {
        return namee;
    }

    public void setNamee(String namee) {
        this.namee = namee;
    }

    public String getImagee() {
        return imagee;
    }

    public void setImagee(String imagee) {
        this.imagee = imagee;
    }

    public String getPhonee() {
        return phonee;
    }

    public void setPhonee(String phonee) {
        this.phonee = phonee;
    }

    public Friend(String idChatRoom) {
        this.idChatRoom = idChatRoom;
    }

    public String getIdChatRoom() {
        return idChatRoom;
    }

    public void setIdChatRoom(String idChatRoom) {
        this.idChatRoom = idChatRoom;
    }
}
