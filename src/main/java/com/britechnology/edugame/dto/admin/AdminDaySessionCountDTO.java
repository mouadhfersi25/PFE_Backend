package com.britechnology.edugame.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDaySessionCountDTO {
    /** Libellé court du jour (ex. Mon, Tue) aligné sur les 7 derniers jours. */
    private String day;
    /** Date ISO (yyyy-MM-dd). */
    private String date;
    private long sessions;
}
