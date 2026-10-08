package org.module.publish_service.request;

import lombok.Data;

import java.util.List;

/**
 * @author : Ly LeangSeng
 * @email : lyleangseng712@gmail.com
 * @date : 10/4/2026 3:05 PM
 */
@Data
public class BaseInternalRequestFilter {
    private Boolean isActivate = true;
    private String search;
    private List<String> fetches;
}
