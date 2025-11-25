package repository.jdbc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.repository.jdbc.JdbcPetTypeRepositoryImpl;
import org.springframework.test.util.ReflectionTestUtils;

class JdbcPetTypeRepositoryImplTests {

    @Mock private DataSource dataSource;
    @Mock private NamedParameterJdbcTemplate template;
    @Mock private SimpleJdbcInsert insert;

    private JdbcPetTypeRepositoryImpl repo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repo = new JdbcPetTypeRepositoryImpl(dataSource);
        ReflectionTestUtils.setField(repo, "namedParameterJdbcTemplate", template);
        ReflectionTestUtils.setField(repo, "insertPetType", insert);
    }

    @Test
    void findByName_returnsPetType_whenExists() {
        PetType expected = new PetType();
        expected.setId(7);
        expected.setName("hamster");

        when(template.queryForObject(
            eq("SELECT id, name FROM types WHERE name= :name"),
            anyMap(),
            any(BeanPropertyRowMapper.class))
        ).thenReturn(expected);

        PetType result = repo.findByName("hamster");

        assertNotNull(result);
        assertEquals(7, result.getId());
        assertEquals("hamster", result.getName());
        verify(template).queryForObject(anyString(), anyMap(), any(BeanPropertyRowMapper.class));
    }

    @Test
    void findByName_throwsObjectRetrievalFailure_whenEmpty() {
        when(template.queryForObject(
            eq("SELECT id, name FROM types WHERE name= :name"),
            anyMap(),
            any(BeanPropertyRowMapper.class))
        ).thenThrow(new EmptyResultDataAccessException(1));

        assertThrows(ObjectRetrievalFailureException.class,
            () -> repo.findByName("no-existe"));
    }

    @Test
    void save_inserts_whenNew_setsId() {
        PetType pt = new PetType(); // id nulo => insert
        pt.setName("iguana");

        when(insert.executeAndReturnKey(any(SqlParameterSource.class))).thenReturn(5);

        repo.save(pt);

        assertEquals(5, pt.getId());
        verify(insert, times(1)).executeAndReturnKey(any(SqlParameterSource.class));
        verify(template, never())
            .update(eq("UPDATE types SET name=:name WHERE id=:id"), any(SqlParameterSource.class));
    }

    @Test
    void save_updates_whenExisting() {
        PetType pt = new PetType();
        pt.setId(9); // update
        pt.setName("doge");

        repo.save(pt);

        verify(template, times(1))
            .update(eq("UPDATE types SET name=:name WHERE id=:id"), any(SqlParameterSource.class));
        verify(insert, never()).executeAndReturnKey(any(SqlParameterSource.class));
    }

    @Test
    void delete_typeWithoutPets_deletesTypeRow() {
        PetType pt = new PetType();
        pt.setId(10);

        when(template.query(
            eq("SELECT pets.id, name, birth_date, type_id, owner_id FROM pets WHERE type_id=:id"),
            anyMap(),
            any(BeanPropertyRowMapper.class)))
            .thenReturn(Collections.emptyList());

        repo.delete(pt);

        verify(template).update(eq("DELETE FROM types WHERE id=:id"), anyMap());
    }
}
