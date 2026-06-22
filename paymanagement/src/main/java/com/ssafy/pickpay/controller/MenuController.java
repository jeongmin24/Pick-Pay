package com.ssafy.pickpay.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.pickpay.dto.MenuResponseDTO;
import com.ssafy.pickpay.service.MenuService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {
	private final MenuService menuService;
	
	@GetMapping
	public ResponseEntity<List<MenuResponseDTO>> getAllMenus() {
		List<MenuResponseDTO> menus = menuService.findAllMenus();
		return ResponseEntity.ok(menus);
	}
	
	@GetMapping("/{menuId}")
	public ResponseEntity<MenuResponseDTO> getMenu(@PathVariable Long menuId) {
		MenuResponseDTO menu = menuService.findMenu(menuId);
		return ResponseEntity.ok(menu);
	}
	
	@PatchMapping("/{menuId}")
	public ResponseEntity<Void> decreaseStock(@PathVariable Long menuId, @RequestParam int quantity) {
		menuService.decreaseStock(menuId, quantity);
		return ResponseEntity.ok().build();
	}
}
