package com.ssafy.pickpay.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.pickpay.domain.Menu;
import com.ssafy.pickpay.dto.MenuResponseDTO;
import com.ssafy.pickpay.repository.MenuRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MenuService {
	
	private final MenuRepository menuRepository;
	
	public List<MenuResponseDTO> findAllMenus(String baseUrl) {
		return menuRepository.findAll().stream()
				.map(menu -> new MenuResponseDTO(menu, baseUrl))
				.collect(Collectors.toList());
	}
	
	public MenuResponseDTO findMenu(Long menuId, String baseUrl) {
		return menuRepository.findById(menuId)
				.map(menu -> new MenuResponseDTO(menu, baseUrl))
				.orElseThrow(() -> new IllegalArgumentException("해당 메뉴가 존재하지 않습니다."));
	}

	@Transactional
	public void decreaseStock(Long menuId, int quantity) {
		Menu menu = menuRepository.findById(menuId)
				.orElseThrow(() -> new IllegalArgumentException("해당 메뉴가 존재하지 않습니다."));
		
		menu.decreaseStock(quantity);
	}
	
	
}
