package com.example.banque_service.controllers;

import com.example.banque_service.entities.*;
import com.example.banque_service.repositories.CompteRepository;
import com.example.banque_service.repositories.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
public class CompteControllerGraphQL {

    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;

    // ========== QUERIES POUR COMPTES ==========

    @QueryMapping
    public List<Compte> allComptes() {
        return compteRepository.findAll();
    }

    @QueryMapping
    public Compte compteById(@Argument Long id) {
        return compteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte avec ID " + id + " non trouvé"));
    }

    @QueryMapping
    public Map<String, Object> totalSolde() {
        long count = compteRepository.count();
        Double sum = compteRepository.sumSoldes();
        Double average = compteRepository.averageSolde();

        return Map.of(
                "count", count,
                "sum", sum != null ? sum : 0.0,
                "average", average != null ? average : 0.0
        );
    }

    // ========== QUERIES POUR TRANSACTIONS ==========

    @QueryMapping
    public List<Transaction> allTransactions() {
        return transactionRepository.findAll();
    }

    @QueryMapping
    public List<Transaction> compteTransactions(@Argument Long id) {
        Compte compte = compteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compte avec ID " + id + " non trouvé"));
        return transactionRepository.findByCompte(compte);
    }

    @QueryMapping
    public Map<String, Object> transactionStats() {
        long count = transactionRepository.count();
        Double sumDepots = transactionRepository.sumByType(TypeTransaction.DEPOT);
        Double sumRetraits = transactionRepository.sumByType(TypeTransaction.RETRAIT);

        return Map.of(
                "count", count,
                "sumDepots", sumDepots != null ? sumDepots : 0.0,
                "sumRetraits", sumRetraits != null ? sumRetraits : 0.0
        );
    }

    // ========== MUTATIONS ==========

    @MutationMapping
    public Compte saveCompte(@Argument Double solde,
                             @Argument String dateCreation,
                             @Argument TypeCompte type) {
        Compte compte = new Compte();
        compte.setSolde(solde != null ? solde : 0.0);

        if (dateCreation != null && !dateCreation.isEmpty()) {
            // Simple parsing de date (vous pouvez améliorer ceci)
            compte.setDateCreation(new Date());
        } else {
            compte.setDateCreation(new Date());
        }

        compte.setType(type != null ? type : TypeCompte.COURANT);

        return compteRepository.save(compte);
    }

    @MutationMapping
    public Transaction addTransaction(@Argument Long compteId,
                                      @Argument Double montant,
                                      @Argument String date,
                                      @Argument TypeTransaction type) {
        // Vérifier que le compte existe
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte avec ID " + compteId + " non trouvé"));

        // Créer la transaction
        Transaction transaction = new Transaction();
        transaction.setMontant(montant != null ? montant : 0.0);
        transaction.setDate(new Date()); // Utilise la date actuelle
        transaction.setType(type != null ? type : TypeTransaction.DEPOT);
        transaction.setCompte(compte);

        // Mettre à jour le solde du compte
        if (type == TypeTransaction.DEPOT) {
            compte.setSolde(compte.getSolde() + montant);
        } else if (type == TypeTransaction.RETRAIT) {
            // Vérifier le solde suffisant
            if (compte.getSolde() < montant) {
                throw new RuntimeException("Solde insuffisant pour effectuer le retrait");
            }
            compte.setSolde(compte.getSolde() - montant);
        }

        // Sauvegarder
        compteRepository.save(compte);
        return transactionRepository.save(transaction);
    }
}