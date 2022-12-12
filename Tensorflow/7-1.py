import matplotlib.pyplot as plt
import tensorflow._api.v2.compat.v1 as tf
tf.disable_v2_behavior()
tf.set_random_seed(777)

x_train_data = [[1, 2, 1], [1, 3, 2], [1, 3, 4], [1, 5, 5], [1, 7, 5], [1, 2, 5], [1, 6, 6], [1, 7, 7]]
y_train_data = [[0, 0, 1], [0, 0, 1], [0, 0, 1], [0, 1, 0], [0, 1, 0], [0, 1, 0], [1, 0, 0], [1, 0, 0]]

x_test = [[2, 1, 1], [3, 1, 2], [3, 3, 4]]
y_test = [[0, 0, 1], [0, 0, 1],[0, 0, 1]]

X = tf.placeholder("float", [None, 3])
Y = tf.placeholder("float", [None, 3])

W = tf.Variable(tf.random_normal([3, 3]))
b = tf.Variable(tf.random_normal([3]))

hypothesis = tf.nn.softmax(tf.matmul(X, W) + b)

cost = tf.reduce_mean(-tf.reduce_sum(Y * tf.log(hypothesis), axis=1))
optimizer = tf.train.GradientDescentOptimizer(learning_rate=0.1).minimize(cost)

prediction = tf.argmax(hypothesis, 1) # 예측한 값
is_correct = tf.equal(prediction, tf.argmax(Y, 1)) # 예측한게 맞는지 확인
accuracy = tf.reduce_mean(tf.cast(is_correct, tf.float32)) # 평균내서 정확도를 구한다

with tf.Session() as sess:
    sess.run(tf.global_variables_initializer())

    for step in range(201):
        cost_val, W_val, _ = sess.run([cost, W, optimizer], feed_dict={X: x_train_data, Y: y_train_data}) # training data만 이용하여서 학습
        print(step, cost_val, W_val)

    print("Prediction:", sess.run(prediction, feed_dict={X: x_test})) # prediction 할 때는 test data set을 활용
    print("Accuracy: ", sess.run(accuracy, feed_dict={X: x_test, Y: y_test})) # accuracy를 측정할 때는 test data set 활용