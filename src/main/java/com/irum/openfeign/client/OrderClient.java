package com.irum.openfeign.client;

import com.irum.openfeign.dto.response.OrderDetailDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderClient {

    @GetMapping("/internal/order-details/{orderDetailId}")
    OrderDetailDto getOrderDetail(@PathVariable("orderDetailId") UUID orderDetailId);
}
