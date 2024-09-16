package com.java.streaming.doa;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CustomMessage {

    private String message ;
    private boolean success = false;
}
