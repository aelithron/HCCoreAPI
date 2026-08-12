package com.hackclub.hccoreapi.DataTypes;

import java.util.UUID;

public record PlayerInfo(UUID uuid, String slack, Nickname nick) {}
