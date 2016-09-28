package kr.ac.kaist.resl.cmsp.iotapp.thing;

import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.AccelService;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.ButtonService;
import kr.ac.kaist.resl.cmsp.iotapp.library.service.general.ThingService;

/**
 * Created by shheo on 15. 7. 20.
 */
public interface SensorTagService extends ThingService, ButtonService, AccelService {
}
