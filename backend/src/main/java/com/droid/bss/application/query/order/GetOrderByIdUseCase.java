package com.droid.bss.application.query.order;

import com.droid.bss.application.dto.order.GetOrderByIdQuery;
import com.droid.bss.application.dto.order.OrderDto;
import com.droid.bss.domain.order.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case to get order by ID
 */
@Service
public class GetOrderByIdUseCase {

    private final OrderRepository orderRepository;

    public GetOrderByIdUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<OrderDto> handle(GetOrderByIdQuery query) {
        return orderRepository.findById(query.getOrderId())
            .map(OrderDto::from);
    }
}
