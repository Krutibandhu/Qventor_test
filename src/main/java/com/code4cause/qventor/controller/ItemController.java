package com.code4cause.qventor.controller;

import com.code4cause.qventor.model.ExportRecord;
import com.code4cause.qventor.model.ImportRecord;
import com.code4cause.qventor.model.Item;
import com.code4cause.qventor.model.Warehouse;
import com.code4cause.qventor.service.ItemService;
import com.code4cause.qventor.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService itemService;
    private final WarehouseService warehouseService;

    @Autowired
    public ItemController(ItemService itemService, WarehouseService warehouseService) {
        this.itemService = itemService;
        this.warehouseService = warehouseService;
    }

    //  Add item to admin by supabaseUserId
    @PostMapping("/save/{supabaseUserId}")
    public ResponseEntity<Item> addItemToAdmin(
            @PathVariable String supabaseUserId,
            @RequestBody Item item
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        if (!supabaseUserId.equals(authenticatedUserId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        Item savedItem = itemService.addItemToAdmin(supabaseUserId, item);
        return ResponseEntity.ok(savedItem);
    }

    //  Search endpoint
    @GetMapping("/search")
    public ResponseEntity<List<Item>> searchItems(@RequestParam("q") String query) {
        List<Item> items = itemService.searchItems(query);
        return ResponseEntity.ok(items);
    }

    //  Get all items of a specific admin
    @GetMapping("/admin/{supabaseUserId}")
    public ResponseEntity<List<Item>> getItemsByAdmin(@PathVariable String supabaseUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        if (!supabaseUserId.equals(authenticatedUserId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(itemService.getItemsByAdmin(supabaseUserId));
    }

    //  Get all items of a specific warehouse (only if user is admin of that warehouse)
    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<Set<Item>> getItemsByWarehouse(@PathVariable Long warehouseId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Warehouse warehouse = warehouseService.getWarehouseById(warehouseId);
        boolean isAdmin = false;
        if (warehouse != null && warehouse.getId() != null) {
            // Check if any admin has this warehouse
            // Since Warehouse does not have direct admin, check via items' admins
            Set<Item> items = warehouse.getItems();
            for (Item item : items) {
                if (item.getAdmin() != null && authenticatedUserId.equals(item.getAdmin().getSupabaseUserId())) {
                    isAdmin = true;
                    break;
                }
            }
        }
        if (!isAdmin) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(warehouseService.getItemsFromWarehouse(warehouseId));
    }

    //  Get single item by ID (only if user is admin of the item)
    @GetMapping("/{itemId}")
    public ResponseEntity<Item> getItemById(@PathVariable Long itemId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Item item = itemService.getItemById(itemId);
        if (item.getAdmin() == null || !authenticatedUserId.equals(item.getAdmin().getSupabaseUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(item);
    }

    //  Update item (only if user is admin of the item)
    @PutMapping("/{itemId}")
    public ResponseEntity<Item> updateItem(@PathVariable Long itemId, @RequestBody Item item) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Item existingItem = itemService.getItemById(itemId);
        if (existingItem.getAdmin() == null || !authenticatedUserId.equals(existingItem.getAdmin().getSupabaseUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(itemService.updateItem(itemId, item));
    }

    //  Delete item (only if user is admin of the item)
    @DeleteMapping("/{itemId}")
    public ResponseEntity<String> deleteItem(@PathVariable Long itemId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Item item = itemService.getItemById(itemId);
        if (item.getAdmin() == null || !authenticatedUserId.equals(item.getAdmin().getSupabaseUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        itemService.deleteItem(itemId);
        return ResponseEntity.ok("Item deleted successfully");
    }

    //  Create: add a new ImportRecord to an existing item (only if user is admin of the item)
    @PostMapping("/{itemId}/imports")
    public ResponseEntity<ImportRecord> addImportToItem(
            @PathVariable Long itemId,
            @RequestBody ImportRecord importRecord
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Item item = itemService.getItemById(itemId);
        if (item.getAdmin() == null || !authenticatedUserId.equals(item.getAdmin().getSupabaseUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(itemService.addImportToItem(itemId, importRecord));
    }

    //  Create: add a new ExportRecord to an existing item (only if user is admin of the item)
    @PostMapping("/{itemId}/exports")
    public ResponseEntity<ExportRecord> addExportToItem(
            @PathVariable Long itemId,
            @RequestBody ExportRecord exportRecord
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Item item = itemService.getItemById(itemId);
        if (item.getAdmin() == null || !authenticatedUserId.equals(item.getAdmin().getSupabaseUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(itemService.addExportToItem(itemId, exportRecord));
    }

    //  Get all import records for an item (only if user is admin of the item)
    @GetMapping("/{itemId}/imports")
    public ResponseEntity<List<ImportRecord>> getImportRecords(@PathVariable Long itemId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Item item = itemService.getItemById(itemId);
        if (item.getAdmin() == null || !authenticatedUserId.equals(item.getAdmin().getSupabaseUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(itemService.getImportRecordsByItem(itemId));
    }

    //  Get single import record (only if user is admin of the item)
    @GetMapping("/{itemId}/imports/{importId}")
    public ResponseEntity<ImportRecord> getSingleImport(
            @PathVariable Long itemId,
            @PathVariable Long importId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Item item = itemService.getItemById(itemId);
        if (item.getAdmin() == null || !authenticatedUserId.equals(item.getAdmin().getSupabaseUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(itemService.getSingleImportRecord(itemId, importId));
    }

    //  Get all export records for an item (only if user is admin of the item)
    @GetMapping("/{itemId}/exports")
    public ResponseEntity<List<ExportRecord>> getExportRecords(@PathVariable Long itemId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Item item = itemService.getItemById(itemId);
        if (item.getAdmin() == null || !authenticatedUserId.equals(item.getAdmin().getSupabaseUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(itemService.getExportRecordsByItem(itemId));
    }

    //  Get single export record (only if user is admin of the item)
    @GetMapping("/{itemId}/exports/{exportId}")
    public ResponseEntity<ExportRecord> getSingleExport(
            @PathVariable Long itemId,
            @PathVariable Long exportId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String authenticatedUserId = authentication.getName();
        Item item = itemService.getItemById(itemId);
        if (item.getAdmin() == null || !authenticatedUserId.equals(item.getAdmin().getSupabaseUserId())) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(itemService.getSingleExportRecord(itemId, exportId));
    }
}
