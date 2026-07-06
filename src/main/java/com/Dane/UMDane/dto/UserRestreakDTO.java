package com.Dane.UMDane.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserRestreakDTO {
    private int restreaksAvailable;
    private List<String> earnedDates;
    private List<String> usedDates;
}
