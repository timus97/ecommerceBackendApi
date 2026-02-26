package com.masai.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

import com.masai.exception.CategoryNotFoundException;
import com.masai.exception.ProductNotFoundException;
import com.masai.models.CategoryEnum;
import com.masai.models.Product;
import com.masai.dto.ProductDTO;
import com.masai.dto.ProductSearchFilterDTO;
import com.masai.dto.ProductSearchResponseDTO;
import com.masai.models.ProductStatus;
import com.masai.models.Seller;
import com.masai.repository.ProductDao;
import com.masai.repository.SellerDao;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductServiceImpl implements ProductService {

	@Autowired
	private ProductDao prodDao;

	@Autowired
	private SellerService sService;

	@Autowired
	private SellerDao sDao;

	@Override
	@Transactional
	public Product addProductToCatalog(String token, Product product) {

		Product prod = null;
		Seller seller1 = sService.getCurrentlyLoggedInSeller(token);
		product.setSeller(seller1);

		Seller Existingseller = sService.getSellerByMobile(product.getSeller().getMobile(), token);
		Optional<Seller> opt = sDao.findById(Existingseller.getSellerId());

		if (opt.isPresent()) {
			Seller seller = opt.get();

			product.setSeller(seller);

			prod = prodDao.save(product);
			;

			seller.getProduct().add(product);
			sDao.save(seller);

		} else {
			prod = prodDao.save(product);
			;
		}

		return prod;
	}

	@Override
	public Product getProductFromCatalogById(Integer id) throws ProductNotFoundException {

		Optional<Product> opt = prodDao.findById(id);
		if (opt.isPresent()) {
			return opt.get();
		}

		else
			throw new ProductNotFoundException("Product not found with given id");
	}

	@Override
	@Transactional
	public String deleteProductFromCatalog(Integer id) throws ProductNotFoundException {
		Optional<Product> opt = prodDao.findById(id);
		
		if (opt.isPresent()) {
			Product prod = opt.get();
			prodDao.delete(prod);
			return "Product deleted from catalog";
		} else
			throw new ProductNotFoundException("Product not found with given id");

	}

	@Override
	@Transactional
	public Product updateProductIncatalog(Product prod) throws ProductNotFoundException {

		Optional<Product> opt = prodDao.findById(prod.getProductId());

		if (opt.isPresent()) {
			opt.get();
			Product prod1 = prodDao.save(prod);
			return prod1;
		} else
			throw new ProductNotFoundException("Product not found with given id");
	}

	@Override
	public List<Product> getAllProductsIncatalog() {
		List<Product> list = prodDao.findAll();
		
		if (list.size() > 0) {
			return list;
		} else
			throw new ProductNotFoundException("No products in catalog");

	}

	@Override
	public List<ProductDTO> getProductsOfCategory(CategoryEnum catenum) {

		List<ProductDTO> list = prodDao.getAllProductsInACategory(catenum);
		if (list.size() > 0) {

			return list;
		} else
			throw new CategoryNotFoundException("No products found with category:" + catenum);
	}

	@Override
	public List<ProductDTO> getProductsOfStatus(ProductStatus status) {

		List<ProductDTO> list = prodDao.getProductsWithStatus(status);

		if (list.size() > 0) {
			return list;
		} else
			throw new ProductNotFoundException("No products found with given status:" + status);
	}

	@Override
	@Transactional
	public Product updateProductQuantityWithId(Integer id,ProductDTO prodDto) {
		Product prod = null;
		 Optional<Product> opt = prodDao.findById(id);
		 
		 if(opt!=null) {
			  prod = opt.get();
			 prod.setQuantity(prod.getQuantity()+prodDto.getQuantity());
			 if(prod.getQuantity()>0) {
				 prod.setStatus(ProductStatus.AVAILABLE);
			 }
			 prodDao.save(prod);
			 
		 }
		 else
			 throw new ProductNotFoundException("No product found with this Id");
		
		return prod;
	}

	

	@Override
	public List<ProductDTO> getAllProductsOfSeller(Integer id) {
		
		List<ProductDTO> list = prodDao.getProductsOfASeller(id);
		
		if(list.size()>0) {
			
			return list;
			
		}
		
		else {
			throw new ProductNotFoundException("No products with SellerId: "+id);
		}
	}

	@Override
	public Map<String, Object> searchAndFilterProducts(ProductSearchFilterDTO filterDTO) {
		
		// Validate and set default pagination values
		Integer page = filterDTO.getPage() != null ? filterDTO.getPage() : 0;
		Integer size = filterDTO.getSize() != null ? filterDTO.getSize() : 10;
		
		// Ensure page and size are positive
		if (page < 0) page = 0;
		if (size <= 0) size = 10;
		
		// Create Pageable object for pagination
		Pageable pageable = PageRequest.of(page, size);
		
		// Call the repository method with all filter parameters
		Page<ProductSearchResponseDTO> results = prodDao.searchAndFilterProducts(
				filterDTO.getKeyword(),
				filterDTO.getCategory(),
				filterDTO.getStatus(),
				filterDTO.getMinPrice(),
				filterDTO.getMaxPrice(),
				filterDTO.getMinRating(),
				filterDTO.getManufacturer(),
				filterDTO.getSellerId(),
				pageable
		);
		
		// If no results found, throw exception
		if (results.isEmpty()) {
			throw new ProductNotFoundException("No products found matching the search and filter criteria");
		}
		
		// Build response map with pagination metadata
		Map<String, Object> response = new HashMap<>();
		response.put("content", results.getContent());
		response.put("totalElements", results.getTotalElements());
		response.put("totalPages", results.getTotalPages());
		response.put("currentPage", results.getNumber());
		response.put("pageSize", results.getSize());
		response.put("hasNextPage", results.hasNext());
		response.put("hasPreviousPage", results.hasPrevious());
		
		return response;
	}

}
