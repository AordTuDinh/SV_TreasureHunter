package ozudo.base.helper;

import javax.persistence.EntityManager;

public interface UpdateCallBackDAO {

    Object onUpdate(EntityManager em);

}
