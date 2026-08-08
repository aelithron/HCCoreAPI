package com.hackclub.hccoreapi.DataTypes;

import java.util.UUID;

public class PlayerInfo {
    public UUID uuid;
    public String slack;
    public Nickname nick;
    public PlayerInfo(UUID uuid, String slack, Nickname nick) {
        this.uuid = uuid;
        this.slack = slack;
        this.nick = nick;
    }
}
