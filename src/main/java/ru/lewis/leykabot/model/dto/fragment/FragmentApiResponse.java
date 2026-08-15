package ru.lewis.leykabot.model.dto.fragment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FragmentApiResponse {
    private boolean ok;
    private String message;
    private String code;
    private Object data;
    private Object result;
}
