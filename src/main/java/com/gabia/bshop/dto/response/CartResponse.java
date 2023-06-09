package com.gabia.bshop.dto.response;

import lombok.Builder;

@Builder
public record CartResponse(
	Long itemId,
	Long itemOptionId,
	int orderCount,
	String itemOptionDescription,
	String itemName,
	int basePrice,
	int optionPrice,
	int stockQuantity,
	String category,
	String itemThumbnailUrl
) {
}
