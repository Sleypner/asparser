package dev.sleypner.asparser.service.parser.fortress.persistence;

import dev.sleypner.asparser.domain.model.FortressSkill;
import dev.sleypner.asparser.service.parser.shared.DateRepository;
import dev.sleypner.asparser.service.parser.shared.RepositoryManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
@Transactional
public class FortressSkillsPersistenceImpl implements FortressSkillsPersistence, RepositoryManager<FortressSkill>, DateRepository<FortressSkill> {

    private final EntityManager em;

    @Autowired
    public FortressSkillsPersistenceImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<FortressSkill> getAll() {
        TypedQuery<FortressSkill> query = em.createQuery("FROM FortressSkill skills", getEntityClass());
        return query.getResultList();
    }

    @Override
    public FortressSkill save(FortressSkill skill) {
        if (!em.contains(skill)) {
            em.persist(skill);
            return skill;
        }
        return em.merge(skill);
    }

    @Override
    public FortressSkill update(FortressSkill skill) {
        return em.merge(skill);
    }

    @Override
    public FortressSkill getById(int id) {
        TypedQuery<FortressSkill> query = em.createQuery("SELECT s FROM FortressSkill s WHERE s.id = :id", getEntityClass());
        query.setParameter("id", id);
        return query.getSingleResult();
    }

    @Override
    public FortressSkill getByName(String name) {
        TypedQuery<FortressSkill> query = em.createQuery("SELECT s FROM FortressSkill s WHERE s.name LIKE :name", getEntityClass());
        query.setParameter("name", name);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public Set<FortressSkill> save(Set<FortressSkill> set) {
        return Set.of();
    }

    @Override
    public void delete(FortressSkill entity) {

    }

    @Override
    public FortressSkill getById(Integer id) {
        return em.find(getEntityClass(), id);
    }

    @Override
    public EntityManager getEm() {
        return em;
    }

    @Override
    public Class<FortressSkill> getEntityClass() {
        return FortressSkill.class;
    }
}
