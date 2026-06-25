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

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {
	private final MenuService menuService;
	
	@GetMapping
	public ResponseEntity<List<MenuResponseDTO>> getAllMenus(HttpServletRequest request) {
		String baseUrl = getBaseUrl(request); 
		
		List<MenuResponseDTO> menus = menuService.findAllMenus(baseUrl);
		return ResponseEntity.ok(menus);
	}
	
	@GetMapping("/{menuId}")
	public ResponseEntity<MenuResponseDTO> getMenu(@PathVariable Long menuId, HttpServletRequest request) {
		String baseUrl = getBaseUrl(request); 
		
		MenuResponseDTO menu = menuService.findMenu(menuId, baseUrl);
		return ResponseEntity.ok(menu);
	}
	
	@GetMapping("/name/{name}")
	public ResponseEntity<MenuResponseDTO> getMenuByName(@PathVariable String name, HttpServletRequest request) {
		String baseUrl = getBaseUrl(request);
		MenuResponseDTO menu = menuService.findMenuByName(name, baseUrl);
		return ResponseEntity.ok(menu);
	}
	
	@PatchMapping("/{menuId}")
	public ResponseEntity<Void> decreaseStock(@PathVariable Long menuId, @RequestParam int quantity) {
		menuService.decreaseStock(menuId, quantity);
		return ResponseEntity.ok().build();
	}
	
	private String getBaseUrl(HttpServletRequest request) {
		return request.getRequestURL().toString().replace(request.getRequestURI(), "");
	}
}
