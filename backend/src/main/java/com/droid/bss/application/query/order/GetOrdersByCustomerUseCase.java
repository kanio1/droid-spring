package com.droid.bss.application.query.order;

import com.droid.bss.application.dto.order.GetOrdersByCustomerQuery;
import com.droid.bss.application.dto.order.OrderDto;
import com.droid.bss.domain.order.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case to get orders by customer
 */
@Service
public class GetOrdersByCustomerUseCase {

    private final OrderRepository orderRepository;

    public GetOrdersByCustomerUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderDto> handle(GetOrdersByCustomerQuery query) {
        return orderRepository.findByCustomerId(query.getCustomerId())
            .stream()
            .map(OrderDto::from)
            .toList();
    }
}
