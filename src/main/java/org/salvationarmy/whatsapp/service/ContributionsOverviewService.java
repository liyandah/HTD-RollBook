package org.salvationarmy.whatsapp.service;

import org.salvationarmy.whatsapp.dto.ContributionsOverviewResponse;
import org.salvationarmy.whatsapp.dto.PaymentResponse;
import org.salvationarmy.whatsapp.entity.ContributionCategory;
import org.salvationarmy.whatsapp.repository.ContributionCategoryRepository;
import org.salvationarmy.whatsapp.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContributionsOverviewService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ContributionCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public ContributionsOverviewResponse getOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.with(LocalTime.MIN);
        LocalDateTime startOfWeek = startOfToday.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfMonth = startOfToday.withDayOfMonth(1);

        // Calculate totals
        BigDecimal totalToday = paymentRepository.sumAmountByDateRange(startOfToday, now.plusDays(1));
        BigDecimal totalThisWeek = paymentRepository.sumAmountByDateRange(startOfWeek, now.plusDays(1));
        BigDecimal totalThisMonth = paymentRepository.sumAmountByDateRange(startOfMonth, now.plusDays(1));

        // Get category totals for this month
        List<ContributionCategory> categories = categoryRepository.findAll();
        Map<String, BigDecimal> collectionsByCategory = new HashMap<>();
        BigDecimal totalTithes = BigDecimal.ZERO;
        BigDecimal totalProjects = BigDecimal.ZERO;
        BigDecimal totalEvents = BigDecimal.ZERO;

        for (ContributionCategory category : categories) {
            BigDecimal categoryTotal = paymentRepository.sumAmountByCategoryAndDateRange(
                    category.getId(), startOfMonth, now.plusDays(1));
            if (categoryTotal == null) categoryTotal = BigDecimal.ZERO;
            
            collectionsByCategory.put(category.getName(), categoryTotal);
            
            ContributionCategory.CategoryType catType = category.getType();
            if (catType == ContributionCategory.CategoryType.TITHE) {
                totalTithes = totalTithes.add(categoryTotal);
            } else if (catType == ContributionCategory.CategoryType.PROJECT) {
                totalProjects = totalProjects.add(categoryTotal);
            } else if (catType == ContributionCategory.CategoryType.EVENT) {
                totalEvents = totalEvents.add(categoryTotal);
            }
        }

        // Get latest payments
        List<PaymentResponse> latestPayments = paymentService.getPayments(PageRequest.of(0, 20))
                .getContent();

        return ContributionsOverviewResponse.builder()
                .totalToday(totalToday != null ? totalToday : BigDecimal.ZERO)
                .totalThisWeek(totalThisWeek != null ? totalThisWeek : BigDecimal.ZERO)
                .totalThisMonth(totalThisMonth != null ? totalThisMonth : BigDecimal.ZERO)
                .totalTithes(totalTithes)
                .totalProjects(totalProjects)
                .totalEvents(totalEvents)
                .latestPayments(latestPayments.stream()
                        .map(p -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", p.getId());
                            map.put("memberName", p.getMemberName());
                            map.put("categoryName", p.getCategoryName());
                            map.put("amount", p.getAmount());
                            map.put("currency", p.getCurrency());
                            map.put("recordedAt", p.getRecordedAt());
                            return map;
                        })
                        .collect(Collectors.toList()))
                .collectionsByCategory(collectionsByCategory)
                .build();
    }
}
