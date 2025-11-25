package repository.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;

import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.repository.jdbc.JdbcPetTypeRepositoryImpl;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JdbcPetTypeRepositoryImplNewTests {

    private DataSource ds;
    private NamedParameterJdbcTemplate named;
    private SimpleJdbcInsert inserter;
    private JdbcPetTypeRepositoryImpl repo;

    // Utilidad para inyectar mocks en campos privados
    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @BeforeEach
    void setup() throws Exception {
        // Mocks
        ds = mock(DataSource.class);
        named = mock(NamedParameterJdbcTemplate.class);
        inserter = mock(SimpleJdbcInsert.class);

        // Instancia real (el constructor crea sus propias dependencias)
        repo = new JdbcPetTypeRepositoryImpl(ds);

        // Forzamos nuestros mocks en los campos privados
        set(repo, "namedParameterJdbcTemplate", named);
        set(repo, "insertPetType", inserter);
    }

    @Test
    void save_shouldUpdate_whenPetTypeHasId() {
        // given: PetType existente (no es "new")
        PetType pt = new PetType();
        pt.setId(10);
        pt.setName("hamster");

        // when
        repo.save(pt);

        // then: capturamos el SQL y el BeanPropertySqlParameterSource
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<BeanPropertySqlParameterSource> srcCap =
            ArgumentCaptor.forClass(BeanPropertySqlParameterSource.class);

        verify(named, times(1))
            .update(sqlCap.capture(), srcCap.capture());

        assertThat(sqlCap.getValue())
            .contains("UPDATE types SET name=:name WHERE id=:id");

        // Comprobamos que el parameterSource lleva los valores correctos
        BeanPropertySqlParameterSource src = srcCap.getValue();
        assertThat(src.getValue("id")).isEqualTo(10);
        assertThat(src.getValue("name")).isEqualTo("hamster");

        // Y que NO se intentó insertar
        verifyNoInteractions(inserter);
    }
}
