# -*- coding: utf-8 -*-

#table = db(db.sensor1.carId == "66-ZZ-66").delete()
#table = db(db.sensor2.carId == "66-ZZ-66").delete()

#db.sensors.drop()

"""
if db(db.sensors).isempty():
    s2 = db(db.sensor2.id > 0).select()
    for xx in s2:
        id = xx["id"]
        s1 = db(db.sensor1.id == id).select().first()
        db.sensors.validate_and_insert(carId=xx["carId"], carLocation=xx["carLocation"], timeValue=xx["timeValue"],
                                        pm25=s1["pm25"], pm10=s1["pm10"],
                                        temperature=xx["temperature"], gas=xx["gas"], humidity=xx["humidity"], pressure=xx["pressure"], altitude=xx["altitude"],
                                       tags=xx["tags"], classification=xx["classification"]
                                        )
"""
#db( db.sensor2.id > 0 ).update( classification = "0")
#db( db.sensor1.id > 0 ).update( classification = "0

#db( db.sensors.id >= 3243 ).update( tags = "Não existência de fumo, vidros fechados, AC desligado")
