package com.gdg.unimatebackend.team.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeamColor {

    C01("#FFE970"),
    C02("#FFF8D3"),
    C03("#90A3ED"),
    C04("#D9E1FF"),
    C05("#F488D4"),
    C06("#FFD8F3"),
    C07("#FF7A6E"),
    C08("#FBB0A9"),
    C09("#9CE098"),
    C10("#D4FFD1");

    private final String hex;
}
