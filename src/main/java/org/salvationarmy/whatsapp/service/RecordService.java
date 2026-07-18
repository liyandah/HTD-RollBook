package org.salvationarmy.whatsapp.service;

import jakarta.persistence.criteria.Predicate;
import org.salvationarmy.whatsapp.dto.AdminVerifyBulkRequest;
import org.salvationarmy.whatsapp.dto.BulkImportRowResult;
import org.salvationarmy.whatsapp.dto.BulkProxyRegisterRequest;
import org.salvationarmy.whatsapp.dto.BulkProxyRegisterResponse;
import org.salvationarmy.whatsapp.dto.BulkValidateResponse;
import org.salvationarmy.whatsapp.dto.CreateRecordRequest;
import org.salvationarmy.whatsapp.dto.DashboardStatsResponse;
import org.salvationarmy.whatsapp.dto.DependentPayload;
import org.salvationarmy.whatsapp.dto.PendingCandidateResponse;
import org.salvationarmy.whatsapp.dto.ProxyUserPayload;
import org.salvationarmy.whatsapp.dto.SoldierRecordResponse;
import org.salvationarmy.whatsapp.entity.RecordStatus;
import org.salvationarmy.whatsapp.entity.SoldierRecord;
import org.salvationarmy.whatsapp.repository.SoldierRecordRepository;
import org.salvationarmy.whatsapp.repository.UserRepository;
import org.salvationarmy.whatsapp.util.MembershipClassifier;
import org.salvationarmy.whatsapp.util.RegistrationFieldValidator;
import org.salvationarmy.whatsapp.util.CorpsNameUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecordService {

    private static final String DEFAULT_CORPS_NAME = "Highfield Temple";

    @Autowired
    private SoldierRecordRepository soldierRecordRepository;

    @Autowired
    private UserRepository userRepository;

    public DashboardStatsResponse getDashboardStats() {
        long totalRecords = soldierRecordRepository.countByIsActiveTrue();
        long verifiedCount = soldierRecordRepository.countByStatusAndIsActiveTrue(RecordStatus.VERIFIED);
        long declinedCount = soldierRecordRepository.countByStatusAndIsActiveTrue(RecordStatus.DECLINED);
        long inProgressCount = soldierRecordRepository.countByStatusAndIsActiveTrue(RecordStatus.IN_PROGRESS)
                + soldierRecordRepository.countByStatusAndIsActiveTrue(RecordStatus.PENDING);
        long under16Count = soldierRecordRepository.countUnder16();
        long age16AndAboveCount = soldierRecordRepository.countAge16AndAbove();
        long totalChildren = soldierRecordRepository.sumChildrenCount();

        // Get recent 10 submissions
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        List<SoldierRecord> latestRecords = soldierRecordRepository.findRecentRecords(
                last7Days, PageRequest.of(0, 10));

        List<SoldierRecordResponse> latestRecordResponses = latestRecords.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .totalRecords(totalRecords)
                .verifiedCount(verifiedCount)
                .declinedCount(declinedCount)
                .inProgressCount(inProgressCount)
                .completedRecords(verifiedCount) // Alias for frontend compatibility
                .verifiedRecords(verifiedCount) // Alias for frontend compatibility
                .under16Count(under16Count)
                .age16AndAboveCount(age16AndAboveCount)
                .totalChildren(totalChildren)
                .latestRecords(latestRecordResponses)
                .build();
    }

    public BulkValidateResponse validateBulkImportRows(List<CreateRecordRequest> rows) {
        BulkValidateResponse.BulkValidateResponseBuilder out = BulkValidateResponse.builder();
        List<BulkImportRowResult> blocked = new ArrayList<>();
        List<BulkImportRowResult> warnings = new ArrayList<>();
        List<BulkImportRowResult> clean = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            return out.blocked(blocked).warnings(warnings).clean(clean).build();
        }

        Set<String> seenNationalIds = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            CreateRecordRequest req = rows.get(i);
            int rowNum = i + 1;
            LocalDate dob = req.getDob();
            int age = calculateAge(dob);
            String idRaw = req.getIdNumber() == null ? "" : req.getIdNumber().trim();
            String fn = req.getFirstName() == null ? "" : req.getFirstName().trim();
            String ln = req.getFamilyName() == null ? "" : req.getFamilyName().trim();

            if (age >= 18 && idRaw.isEmpty()) {
                blocked.add(BulkImportRowResult.builder()
                        .index(rowNum)
                        .category("BLOCKED")
                        .code("MISSING_ID_NUMBER")
                        .message("ID Number is mandatory for individuals 18 years or older.")
                        .row(req)
                        .build());
                continue;
            }

            if (!idRaw.isEmpty()) {
                String idKey = idRaw.toLowerCase(Locale.ROOT);
                if (seenNationalIds.contains(idKey)) {
                    blocked.add(BulkImportRowResult.builder()
                            .index(rowNum)
                            .category("BLOCKED")
                            .code("DUPLICATE_ID_IN_FILE")
                            .message("National ID appears more than once in this file.")
                            .row(req)
                            .build());
                    continue;
                }
                if (soldierRecordRepository.countByNormalizedIdNumber(idRaw) > 0) {
                    blocked.add(BulkImportRowResult.builder()
                            .index(rowNum)
                            .category("BLOCKED")
                            .code("DUPLICATE_NATIONAL_ID")
                            .message("National ID already registered.")
                            .row(req)
                            .build());
                    continue;
                }
                seenNationalIds.add(idKey);
            }

            if (soldierRecordRepository.countByNormalizedFullName(fn, ln) > 0) {
                String existingId = soldierRecordRepository.findAnyIdNumberByNormalizedFullName(fn, ln);
                String full = (fn + " " + ln).trim();
                String msg = existingId != null
                        ? "Someone named " + full + " is already in the system with a different or existing ID record. Add only if this is a different person."
                        : "A record with the same name already exists. Add only if this is a different person.";
                warnings.add(BulkImportRowResult.builder()
                        .index(rowNum)
                        .category("WARNING")
                        .code("NAME_MATCH")
                        .message(msg)
                        .row(req)
                        .build());
                continue;
            }

            clean.add(BulkImportRowResult.builder()
                    .index(rowNum)
                    .category("CLEAN")
                    .code("CLEAN")
                    .message("Ready to import.")
                    .row(req)
                    .build());
        }

        return out.blocked(blocked).warnings(warnings).clean(clean).build();
    }

    @Transactional
    public int recalculateAllMembershipClassifications() {
        List<SoldierRecord> all = soldierRecordRepository.findAll();
        for (SoldierRecord r : all) {
            if (r.getDob() != null) {
                r.setAge(calculateAge(r.getDob()));
            }
            applyMembershipClassification(r);
        }
        soldierRecordRepository.saveAll(all);
        return all.size();
    }

    public Page<SoldierRecordResponse> getRecords(String status, LocalDate from, LocalDate to,
                                                   String query, String department, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<SoldierRecord> spec = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.trim().isEmpty()) {
                try {
                    RecordStatus recordStatus = RecordStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), recordStatus));
                } catch (IllegalArgumentException e) {
                    // Handle invalid status string, perhaps log it or return an empty page
                    predicates.add(criteriaBuilder.disjunction()); // Ensure no results for invalid status
                }
            }
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), from.atStartOfDay()));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }

            if (query != null && !query.trim().isEmpty()) {
                String likePattern = "%" + query.toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("familyName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("corpsName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ward")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("brigade")), likePattern)
                );
                predicates.add(namePredicate);
            }

            if (department != null && !department.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("department"), department.trim()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<SoldierRecord> records = soldierRecordRepository.findAll(spec, pageable);
        return records.map(this::toResponse);
    }

    public SoldierRecordResponse getRecordById(UUID id) {
        SoldierRecord record = soldierRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        return toResponse(record);
    }

    @Transactional
    public SoldierRecordResponse createRecord(String waId, String corpsName, String enrolledCorpsName,
                                             String ward, String brigade, String firstName, String familyName,
                                             LocalDate dob, String idNumber, String gender, String maritalStatus,
                                             Integer kidsCount, String favoriteSong,
                                             String favoriteBibleVerse, String status) {
        SoldierRecord record = new SoldierRecord();
        record.setWaId(waId);
        record.setCorpsName(DEFAULT_CORPS_NAME);
        record.setEnrolledCorpsName(DEFAULT_CORPS_NAME);
        record.setWard(ward);
        record.setBrigade(brigade);
        record.setFirstName(firstName);
        record.setFamilyName(familyName);
        record.setDob(dob);

        int calculatedAge = calculateAge(dob);
        record.setAge(calculatedAge);

        if (calculatedAge >= 18 && (idNumber == null || idNumber.trim().isEmpty())) {
            throw new IllegalArgumentException("ID Number is mandatory for individuals 18 years or older.");
        }
        record.setIdNumber(idNumber);
        record.setGender(gender);
        record.setMaritalStatus(maritalStatus);
        record.setKidsCount(kidsCount != null ? kidsCount : 0);
        applyMembershipClassification(record);

        record.setFavoriteSong(favoriteSong);
        record.setFavoriteBibleVerse(favoriteBibleVerse);
        record.setStatus(status != null && !status.isEmpty() ? RecordStatus.valueOf(status.toUpperCase()) : RecordStatus.PENDING);
        
        // Generate record_code using sequence with year: HTE-2026-005
        Long seqValue = soldierRecordRepository.getNextRecordSequence();
        int currentYear = Year.now().getValue();
        record.setRecordCode("HTE-" + currentYear + "-" + String.format("%03d", seqValue));
        
        // Set template_type: HIGH_FIELD_TEMPLE if any record exists for this wa_id, else STANDARD
        List<SoldierRecord> existingRecords = soldierRecordRepository.findByWaId(waId);
        if (!existingRecords.isEmpty()) {
            record.setTemplateType("HIGH_FIELD_TEMPLE");
        } else {
            record.setTemplateType("STANDARD");
        }
        
        SoldierRecord savedRecord = soldierRecordRepository.save(record);
        return toResponse(savedRecord);
    }

    /**
     * Admin bulk import: trust spreadsheet data, assign next sequence record code (HTE-YYYY-XXX), force VERIFIED.
     */
    @Transactional
    public SoldierRecordResponse createAdminBulkVerifiedRecord(String waId, String corpsName, String enrolledCorpsName,
                                                               String ward, String brigade, String firstName, String familyName,
                                                               LocalDate dob, String idNumber, String gender, String maritalStatus,
                                                               Integer kidsCount, String favoriteSong,
                                                               String favoriteBibleVerse) {
        SoldierRecord record = new SoldierRecord();
        record.setWaId(waId);
        record.setCorpsName(DEFAULT_CORPS_NAME);
        record.setEnrolledCorpsName(DEFAULT_CORPS_NAME);
        record.setWard(ward);
        record.setBrigade(brigade);
        record.setFirstName(firstName);
        record.setFamilyName(familyName);
        record.setDob(dob);

        int calculatedAge = calculateAge(dob);
        record.setAge(calculatedAge);

        if (calculatedAge >= 18 && (idNumber == null || idNumber.trim().isEmpty())) {
            throw new IllegalArgumentException("ID Number is mandatory for individuals 18 years or older.");
        }
        record.setIdNumber(idNumber);
        record.setGender(gender);
        record.setMaritalStatus(maritalStatus);
        record.setKidsCount(kidsCount != null ? kidsCount : 0);
        applyMembershipClassification(record);

        record.setFavoriteSong(favoriteSong);
        record.setFavoriteBibleVerse(favoriteBibleVerse);
        record.setStatus(RecordStatus.VERIFIED);

        Long seqValue = soldierRecordRepository.getNextRecordSequence();
        int currentYear = Year.now().getValue();
        record.setRecordCode("HTE-" + currentYear + "-" + String.format("%03d", seqValue));

        List<SoldierRecord> existingRecords = soldierRecordRepository.findByWaId(waId);
        if (!existingRecords.isEmpty()) {
            record.setTemplateType("HIGH_FIELD_TEMPLE");
        } else {
            record.setTemplateType("STANDARD");
        }

        SoldierRecord savedRecord = soldierRecordRepository.save(record);
        return toResponse(savedRecord);
    }

    @Transactional
    public SoldierRecordResponse updateRecord(UUID id, String corpsName, String enrolledCorpsName,
                                             String ward, String brigade, String firstName, String familyName,
                                             LocalDate dob, String idNumber, String phoneNumber, String address,
                                             String gender, String maritalStatus,
                                             Integer kidsCount, String nextOfKinName, String nextOfKinPhone, String favoriteSong,
                                             String favoriteBibleVerse, String personImagePath,
                                             String certImagePath, String status) {
        SoldierRecord record = soldierRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        
        record.setCorpsName(DEFAULT_CORPS_NAME);
        record.setEnrolledCorpsName(DEFAULT_CORPS_NAME);
        if (ward != null) record.setWard(ward);
        if (brigade != null) record.setBrigade(brigade);
        if (firstName != null) record.setFirstName(firstName);
        if (familyName != null) record.setFamilyName(familyName);
        if (dob != null) {
            record.setDob(dob);
            record.setAge(calculateAge(dob));
        }

        Integer currentAge = record.getAge();
        boolean hasIncomingIdNumber = idNumber != null && !idNumber.trim().isEmpty();
        String effectiveIdNumber = hasIncomingIdNumber ? idNumber.trim() : record.getIdNumber();
        if (currentAge != null && currentAge >= 18 && (effectiveIdNumber == null || effectiveIdNumber.trim().isEmpty())) {
            throw new IllegalArgumentException("ID Number is mandatory for individuals 18 years or older.");
        }
        if (hasIncomingIdNumber) record.setIdNumber(idNumber.trim());
        if (phoneNumber != null) record.setPhoneNumber(RegistrationFieldValidator.sanitizeRequiredField(phoneNumber));
        if (address != null) record.setAddress(address);
        if (gender != null) record.setGender(gender);
        if (maritalStatus != null) record.setMaritalStatus(maritalStatus);
        if (kidsCount != null) record.setKidsCount(kidsCount);
        if (nextOfKinName != null) record.setNextOfKinName(RegistrationFieldValidator.sanitizeRequiredField(nextOfKinName));
        if (nextOfKinPhone != null) record.setNextOfKinPhone(RegistrationFieldValidator.sanitizeRequiredField(nextOfKinPhone));

        if (favoriteSong != null) record.setFavoriteSong(RegistrationFieldValidator.sanitizeRequiredField(favoriteSong));
        if (favoriteBibleVerse != null) record.setFavoriteBibleVerse(RegistrationFieldValidator.sanitizeRequiredField(favoriteBibleVerse));
        if (personImagePath != null) record.setPersonImagePath(personImagePath);
        if (certImagePath != null) record.setCertImagePath(certImagePath);
        if (status != null) record.setStatus(RecordStatus.valueOf(status.toUpperCase()));

        applyMembershipClassification(record);
        
        soldierRecordRepository.save(record);
        return toResponse(record);
    }

    @Autowired
    private org.salvationarmy.whatsapp.service.ConversationService conversationService;

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Transactional
    public SoldierRecordResponse updateStatus(UUID id, RecordStatus status, String declineReason) {
        SoldierRecord record = soldierRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        RecordStatus oldStatus = record.getStatus();
        record.setStatus(status);
        if (status == RecordStatus.DECLINED && declineReason != null && !declineReason.trim().isEmpty()) {
            record.setHouseholdAdminNotes(declineReason.trim());
        }
        soldierRecordRepository.save(record);
        syncUserVerificationFromRecord(record);
        
        // Notify chat if status changed to VERIFIED (using new chat system)
        if (status == RecordStatus.VERIFIED && oldStatus != RecordStatus.VERIFIED) {
            notifyVerificationChannels(record);
            notifyPrimaryRegistrantOnApproval(record);
        }
        
        return toResponse(record);
    }

    private void notifyPrimaryRegistrantOnApproval(SoldierRecord approvedRecord) {
        if (approvedRecord.getPrimaryRegistrantId() == null) {
            return;
        }
        soldierRecordRepository.findById(approvedRecord.getPrimaryRegistrantId()).ifPresent(primary -> {
            if (primary.getWaId() == null || primary.getWaId().trim().isEmpty()) {
                return;
            }
            String primaryName = formatFullName(primary);
            String approvedName = formatFullName(approvedRecord);
            String relation = approvedRecord.getRegistrationRelation() != null ? approvedRecord.getRegistrationRelation() : "connection";
            String message = String.format(
                    "Hi %s, great news! The registration for your %s, %s, has been approved. Thank you for helping our Corps grow! \uD83D\uDE4F",
                    primaryName.isEmpty() ? "Member" : primaryName,
                    relation,
                    approvedName.isEmpty() ? "member" : approvedName
            );
            whatsAppService.sendMessage(primary.getWaId(), message);
        });
    }

    public Map<String, List<SoldierRecordResponse>> getConnections(UUID id) {
        List<SoldierRecordResponse> family = soldierRecordRepository
                .findByPrimaryRegistrantIdAndRegistrationRelationIgnoreCase(id, "Family")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        List<SoldierRecordResponse> friends = soldierRecordRepository
                .findByPrimaryRegistrantIdAndRegistrationRelationIgnoreCase(id, "Friend")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        Map<String, List<SoldierRecordResponse>> result = new HashMap<>();
        result.put("family", family);
        result.put("friends", friends);
        return result;
    }

    @Transactional
    public boolean requestCertificateUpload(UUID id) {
        SoldierRecord record = soldierRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        record.setCertImagePath(null);
        record.setNeedsReupload(true);
        record.setStatus(RecordStatus.REUPLOAD_REQUIRED);
        soldierRecordRepository.save(record);
        return chatbotService.requestCertificateUpload(id);
    }

    @Transactional
    public boolean requestCertificateReupload(UUID id, String reason) {
        SoldierRecord record = soldierRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        record.setCertImagePath(null);
        record.setNeedsReupload(true);
        record.setStatus(RecordStatus.REUPLOAD_REQUIRED);
        if (reason != null && !reason.trim().isEmpty()) {
            record.setPhotoReviewNotes(reason.trim());
            record.setHouseholdAdminNotes(reason.trim());
        }
        soldierRecordRepository.save(record);
        return chatbotService.requestCertificateUpload(id);
    }
    
    @Autowired
    private org.salvationarmy.whatsapp.repository.RegistrationProfileRepository registrationProfileRepository;
    
    /**
     * Notify user in chat when their record is verified (new chat system)
     * Sets up post-verification flow to collect Ward, Brigade, and images
     */
    private void notifyUserInChat(SoldierRecord record) {
        if (record.getUserId() == null) {
            org.slf4j.LoggerFactory.getLogger(RecordService.class)
                .debug("Record {} has no userId, skipping chat notification", record.getId());
            return;
        }
        
        try {
            // Get or create bot conversation for the user
            org.salvationarmy.whatsapp.entity.ConversationNew botConversation = 
                conversationService.getOrCreateBotConversation(record.getUserId());
            
            // Update registration profile to trigger post-verification flow
            registrationProfileRepository.findByUserId(record.getUserId()).ifPresent(profile -> {
                profile.setRegistrationStep("VERIFIED_NOTIFICATION");
                registrationProfileRepository.save(profile);
            });
            
            // Send verification message and start post-verification flow
            String message = String.format(
                "🎉 Great news! Your registration has been verified!\n\n" +
                "Your Record ID: %s\n\n" +
                "Please provide the following additional information:\n" +
                "What is your Ward?",
                record.getRecordCode()
            );
            
            conversationService.sendBotMessage(botConversation.getId(), message);
            
            org.slf4j.LoggerFactory.getLogger(RecordService.class)
                .info("Sent verification notification to user {} for record {}", 
                    record.getUserId(), record.getRecordCode());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(RecordService.class)
                .error("Error sending verification notification to user {}: {}", 
                    record.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void deleteRecord(UUID id) {
        SoldierRecord record = soldierRecordRepository.findById(id).orElse(null);
        if (record == null) {
            throw new RuntimeException("Record not found");
        }
        record.setIsActive(false);
        record.setStatus(RecordStatus.DECLINED);
        soldierRecordRepository.save(record);
    }

    public List<SoldierRecordResponse> getAllRecordsForExport(String status, LocalDate from,
                                                              LocalDate to, String query, String department) {
        Specification<SoldierRecord> spec = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.trim().isEmpty()) {
                try {
                    RecordStatus recordStatus = RecordStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), recordStatus));
                } catch (IllegalArgumentException e) {
                    predicates.add(criteriaBuilder.disjunction());
                }
            }
            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"), from.atStartOfDay()));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }

            if (query != null && !query.trim().isEmpty()) {
                String likePattern = "%" + query.toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("familyName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("corpsName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ward")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("brigade")), likePattern)
                );
                predicates.add(namePredicate);
            }

            if (department != null && !department.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("department"), department.trim()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<SoldierRecord> records = soldierRecordRepository.findAll(spec, 
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return records.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void applyMembershipClassification(SoldierRecord record) {
        int age = record.getAge() != null ? record.getAge() : calculateAge(record.getDob());
        int kids = record.getKidsCount() != null ? record.getKidsCount() : 0;
        String g = MembershipClassifier.normalizeGender(record.getGender());
        String m = MembershipClassifier.normalizeMarital(record.getMaritalStatus());
        record.setDepartment(MembershipClassifier.classifyDepartment(age, g, m, kids));
        record.setBrigadeEligibility(MembershipClassifier.classifyBrigadeEligibility(age));
    }

    /**
     * Bulk proxy flow: Child under 13 → Junior Soldier; Parent over 60 → Senior Member (60+); else standard rules.
     */
    private void applyHouseholdRelationshipDepartment(SoldierRecord record, String relationshipRaw) {
        int age = record.getAge() != null ? record.getAge() : calculateAge(record.getDob());
        String rel = relationshipRaw == null ? "" : relationshipRaw.trim();
        if ("Child".equalsIgnoreCase(rel) && age < 13) {
            record.setDepartment("Junior Soldier");
            record.setBrigadeEligibility(MembershipClassifier.classifyBrigadeEligibility(age));
            return;
        }
        if ("Parent".equalsIgnoreCase(rel) && age > 60) {
            record.setDepartment("Senior Member (60+)");
            record.setBrigadeEligibility(MembershipClassifier.classifyBrigadeEligibility(age));
            return;
        }
        applyMembershipClassification(record);
    }

    private static String[] splitFullName(String fullName) {
        if (fullName == null) {
            return new String[]{"", ""};
        }
        String t = fullName.trim();
        if (t.isEmpty()) {
            return new String[]{"", ""};
        }
        int sp = t.lastIndexOf(' ');
        if (sp < 0) {
            return new String[]{t, ""};
        }
        return new String[]{t.substring(0, sp).trim(), t.substring(sp + 1).trim()};
    }

    private static LocalDate approximateDobFromAge(int age) {
        int clamped = Math.max(0, Math.min(age, 120));
        return LocalDate.now().minusYears(clamped).withMonth(Month.JUNE.getValue()).withDayOfMonth(15);
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String syntheticWaId(String prefix) {
        String s = prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return s.length() > 50 ? s.substring(0, 50) : s;
    }

    @Transactional
    public BulkProxyRegisterResponse registerBulkProxyHousehold(BulkProxyRegisterRequest request) {
        if (request.getDependents() == null || request.getDependents().isEmpty()) {
            throw new IllegalArgumentException("At least one dependent is required.");
        }
        ProxyUserPayload proxy = request.getProxyUser();
        String idRaw = proxy.getIdNumber() == null ? "" : proxy.getIdNumber().trim();
        if (idRaw.isEmpty()) {
            throw new IllegalArgumentException("Proxy id_number is required.");
        }
        if (soldierRecordRepository.countByNormalizedIdNumber(idRaw) > 0) {
            throw new IllegalArgumentException("A record with this proxy national ID already exists.");
        }

        String[] proxyNames = splitFullName(proxy.getFullName());
        String familyName = proxyNames[1].isEmpty() ? "-" : proxyNames[1];
        String waProxy = syntheticWaId("prox");
        String batchId = "BATCH-" + (1000 + (int) (Math.random() * 9000));
        int currentYear = Year.now().getValue();

        List<SoldierRecordResponse> created = new ArrayList<>();
        int dupBlocked = 0;

        SoldierRecord proxyRow = new SoldierRecord();
        proxyRow.setWaId(waProxy);
        proxyRow.setPhoneNumber(trimOrNull(proxy.getPhone()));
        proxyRow.setAddress(trimOrNull(proxy.getAddress()));
        proxyRow.setIdNumber(idRaw);
        proxyRow.setFirstName(proxyNames[0].isEmpty() ? "-" : proxyNames[0]);
        proxyRow.setFamilyName(familyName);
        LocalDate proxyDob = proxy.getDob() != null ? proxy.getDob()
                : LocalDate.of(Year.now().getValue() - 35, Month.JANUARY, 1);
        proxyRow.setDob(proxyDob);
        proxyRow.setAge(calculateAge(proxyDob));
        proxyRow.setProxyId(null);
        proxyRow.setRelationship(null);
        proxyRow.setHouseholdBatchId(batchId);
        proxyRow.setGender(null);
        proxyRow.setMaritalStatus(null);
        proxyRow.setKidsCount(0);
        applyMembershipClassification(proxyRow);
        proxyRow.setFavoriteSong(null);
        proxyRow.setFavoriteBibleVerse(null);
        proxyRow.setStatus(RecordStatus.PENDING);
        Long seqValue = soldierRecordRepository.getNextRecordSequence();
        proxyRow.setRecordCode("HTE-" + currentYear + "-" + String.format("%03d", seqValue));
        proxyRow.setTemplateType("STANDARD");
        soldierRecordRepository.save(proxyRow);
        created.add(toResponse(proxyRow));

        for (DependentPayload dep : request.getDependents()) {
            String depId = dep.getIdNumber() == null ? "" : dep.getIdNumber().trim();
            int age = dep.getAge() == null ? 0 : dep.getAge();
            if (age >= 18 && depId.isEmpty()) {
                dupBlocked++;
                continue;
            }
            if (!depId.isEmpty() && soldierRecordRepository.countByNormalizedIdNumber(depId) > 0) {
                dupBlocked++;
                continue;
            }
            String[] dn = splitFullName(dep.getFullName());
            String depFamily = dn[1].isEmpty() ? "-" : dn[1];
            SoldierRecord row = new SoldierRecord();
            row.setWaId(syntheticWaId("dep"));
            row.setFirstName(dn[0].isEmpty() ? "-" : dn[0]);
            row.setFamilyName(depFamily);
            LocalDate depDob = approximateDobFromAge(age);
            row.setDob(depDob);
            row.setAge(calculateAge(depDob));
            row.setIdNumber(depId.isEmpty() ? null : depId);
            row.setGender(trimOrNull(dep.getGender()));
            row.setMaritalStatus(null);
            row.setKidsCount(0);
            row.setRelationship(trimOrNull(dep.getRelationship()));
            row.setProxyId(idRaw);
            row.setHouseholdBatchId(batchId);
            String ph = trimOrNull(dep.getPhone());
            row.setPhoneNumber(ph != null ? ph : proxyRow.getPhoneNumber());
            String addr = trimOrNull(dep.getAddress());
            row.setAddress(addr != null ? addr : proxyRow.getAddress());
            row.setFavoriteSong(null);
            row.setFavoriteBibleVerse(trimOrNull(dep.getFavoriteVerse()));
            applyHouseholdRelationshipDepartment(row, dep.getRelationship());
            row.setStatus(RecordStatus.PENDING);
            Long seq = soldierRecordRepository.getNextRecordSequence();
            row.setRecordCode("HTE-" + currentYear + "-" + String.format("%03d", seq));
            row.setTemplateType("STANDARD");
            soldierRecordRepository.save(row);
            created.add(toResponse(row));
        }

        int total = 1 + request.getDependents().size();
        int depsAdded = created.size() - 1;
        return BulkProxyRegisterResponse.builder()
                .totalRowsProcessed(total)
                .newHouseholdHeads(1)
                .dependentsAdded(depsAdded)
                .duplicatesBlocked(dupBlocked)
                .batchId(batchId)
                .records(created)
                .build();
    }

    @Transactional
    public Map<String, Object> verifyBulkHousehold(AdminVerifyBulkRequest req) {
        if (req.getVerifyStatus() == null || !req.getVerifyStatus().trim().equalsIgnoreCase("verified")) {
            throw new IllegalArgumentException("verify_status must be Verified.");
        }
        List<SoldierRecord> targets = new ArrayList<>();
        if (req.getRecordIds() != null && !req.getRecordIds().isEmpty()) {
            for (String code : req.getRecordIds()) {
                if (code == null || code.trim().isEmpty()) {
                    continue;
                }
                soldierRecordRepository.findByRecordCode(code.trim()).ifPresent(targets::add);
            }
        } else if (req.getBatchId() != null && !req.getBatchId().trim().isEmpty()) {
            targets.addAll(soldierRecordRepository.findByHouseholdBatchId(req.getBatchId().trim()));
        } else if (req.getProxyId() != null && !req.getProxyId().trim().isEmpty()) {
            targets.addAll(soldierRecordRepository.findHouseholdByProxyNationalId(req.getProxyId().trim()));
        } else {
            throw new IllegalArgumentException("Provide batch_id, record_ids, or proxy_id (national ID of household head).");
        }
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("No matching records for verification.");
        }

        Map<UUID, RecordStatus> previous = targets.stream()
                .collect(Collectors.toMap(SoldierRecord::getId, SoldierRecord::getStatus));
        String notes = req.getAdminNotes() == null ? null : req.getAdminNotes().trim();
        for (SoldierRecord r : targets) {
            r.setStatus(RecordStatus.VERIFIED);
            if (notes != null && !notes.isEmpty()) {
                r.setHouseholdAdminNotes(notes);
            }
        }
        soldierRecordRepository.saveAll(targets);

        for (SoldierRecord r : targets) {
            RecordStatus was = previous.get(r.getId());
            syncUserVerificationFromRecord(r);
            if (was != RecordStatus.VERIFIED && r.getStatus() == RecordStatus.VERIFIED) {
                notifyVerificationChannels(r);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("updated", targets.size());
        body.put("total_verified", targets.size());
        body.put("record_codes", targets.stream().map(SoldierRecord::getRecordCode).collect(Collectors.toList()));
        return body;
    }

    public List<PendingCandidateResponse> getPendingCandidates() {
        List<SoldierRecord> pending = soldierRecordRepository.findByStatusAndIsActiveTrueOrderByCreatedAtDesc(RecordStatus.PENDING);
        return pending.stream().map(r -> {
            boolean married = r.getMaritalStatus() != null && r.getMaritalStatus().equalsIgnoreCase("married");
            String fullName = ((r.getFirstName() != null ? r.getFirstName().trim() : "") + " "
                    + (r.getFamilyName() != null ? r.getFamilyName().trim() : "")).trim();
            return PendingCandidateResponse.builder()
                    .id(r.getId())
                    .recordCode(r.getRecordCode())
                    .fullName(fullName)
                    .gender(r.getGender())
                    .age(r.getAge())
                    .married(married)
                    .childrenCount(r.getKidsCount() != null ? r.getKidsCount() : 0)
                    .assignedSection(r.getDepartment())
                    .status(r.getStatus() != null ? r.getStatus().name() : null)
                    .registeredBy(r.getProxyId())
                    .createdAt(r.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public SoldierRecordResponse verifyCandidate(UUID id) {
        SoldierRecord record = soldierRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));
        record.setStatus(RecordStatus.VERIFIED);
        soldierRecordRepository.save(record);
        syncUserVerificationFromRecord(record);
        notifyVerificationChannels(record);
        return toResponse(record);
    }

    /**
     * Notify both chat systems:
     * - Internal conversation chat (userId based)
     * - Public chatbot flow (chatSessionId based)
     */
    private void notifyVerificationChannels(SoldierRecord record) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RecordService.class);
        try {
            notifyUserInChat(record);
        } catch (Exception e) {
            logger.warn("Failed to notify internal chat for record {}: {}", record.getId(), e.getMessage(), e);
        }
        try {
            chatbotService.notifyUserOfVerification(record.getId());
        } catch (Exception e) {
            logger.warn("Failed to notify chatbot flow for record {}: {}", record.getId(), e.getMessage(), e);
        }
    }

    private void syncUserVerificationFromRecord(SoldierRecord record) {
        if (record.getUserId() == null) {
            return;
        }
        userRepository.findById(record.getUserId()).ifPresent(user -> {
            user.setGender(record.getGender());
            user.setMaritalStatus(record.getMaritalStatus());
            user.setChildrenCount(record.getKidsCount() != null ? record.getKidsCount() : 0);
            user.setCategory(record.getDepartment());
            boolean verified = record.getStatus() == RecordStatus.VERIFIED;
            user.setIsVerified(verified);
            user.setMemberRole(verified ? "MEMBER" : "CANDIDATE");
            userRepository.save(user);
        });
    }

    private SoldierRecordResponse toResponse(SoldierRecord record) {
        SoldierRecordResponse.SoldierRecordResponseBuilder b = SoldierRecordResponse.builder()
                .id(record.getId())
                .waId(record.getWaId())
                .recordCode(record.getRecordCode())
                .corpsName(CorpsNameUtil.normalize(record.getCorpsName()))
                .enrolledCorpsName(CorpsNameUtil.normalize(record.getEnrolledCorpsName()))
                .ward(record.getWard())
                .brigade(record.getBrigade())
                .firstName(record.getFirstName())
                .familyName(record.getFamilyName())
                .dob(record.getDob())
                .age(record.getAge())
                .idNumber(record.getIdNumber())
                .gender(record.getGender())
                .maritalStatus(record.getMaritalStatus())
                .kidsCount(record.getKidsCount())
                .department(record.getDepartment())
                .brigadeEligibility(record.getBrigadeEligibility())
                .phoneNumber(record.getPhoneNumber())
                .address(record.getAddress())
                .homeAddress(record.getAddress())
                .nextOfKinName(record.getNextOfKinName())
                .nextOfKinPhone(record.getNextOfKinPhone())
                .proxyId(record.getProxyId())
                .relationship(record.getRelationship())
                .primaryRegistrantId(record.getPrimaryRegistrantId())
                .registrationRelation(record.getRegistrationRelation())
                .needsReupload(record.getNeedsReupload())
                .householdBatchId(record.getHouseholdBatchId())
                .householdAdminNotes(record.getHouseholdAdminNotes())
                .favoriteSong(record.getFavoriteSong())
                .favoriteBibleVerse(record.getFavoriteBibleVerse())
                .personImageUrl(record.getPersonImagePath() != null
                        ? "/api/images/" + record.getPersonImagePath() : null)
                .certImageUrl(record.getCertImagePath() != null
                        ? "/api/images/" + record.getCertImagePath() : null)
                .status(record.getStatus() != null ? record.getStatus().name() : null)
                .templateType(record.getTemplateType())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt());

        if (record.getProxyId() != null && !record.getProxyId().trim().isEmpty()) {
            String pid = record.getProxyId().trim();
            Optional<SoldierRecord> proxy = soldierRecordRepository.findFirstByIdNumberIgnoreCase(pid);
            if (proxy.isEmpty()) {
                proxy = soldierRecordRepository.findByRecordCode(pid);
            }
            proxy.ifPresent(p -> b.registeredByName(formatFullName(p))
                    .proxyContact(p.getPhoneNumber()));
        }

        return b.build();
    }

    private static String formatFullName(SoldierRecord proxy) {
        String fn = proxy.getFirstName() != null ? proxy.getFirstName().trim() : "";
        String ln = proxy.getFamilyName() != null ? proxy.getFamilyName().trim() : "";
        return (fn + " " + ln).trim();
    }
    private int calculateAge(LocalDate dob) {
        if (dob == null) {
            return 0;
        }
        return Period.between(dob, LocalDate.now()).getYears();
    }

}


