package ozudo.base.helper;

import javax.persistence.EntityManager;

public interface QueryCallBackDAO {

    Object onQuery(EntityManager em);

}
